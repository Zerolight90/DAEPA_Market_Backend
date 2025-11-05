package com.daepamarket.daepa_market_backend.chat.service;

import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomListDto;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomReq;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomRes;
import com.daepamarket.daepa_market_backend.domain.chat.ChatRoomEntity;
import com.daepamarket.daepa_market_backend.domain.chat.repository.ChatRoomRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper;

    @PersistenceContext
    private EntityManager em;

    private static final String WELCOME =
            "안전한 거래를 위해 개인정보을 포함한 내용은 채팅은 삼가해주세요";

    @Transactional(readOnly = true)
    public List<ChatRoomListDto> listRooms(Long userIdOrNull) {
        return chatRoomMapper.listRooms(userIdOrNull);
    }

    @Transactional(readOnly = true)
    public boolean isParticipant(Long roomId, Long userId) {
        Object exists = em.createNativeQuery("""
            SELECT 1 FROM chat_reads
             WHERE ch_idx = :roomId AND cread_reader = :userId
             LIMIT 1
        """).setParameter("roomId", roomId)
                .setParameter("userId", userId)
                .getResultStream().findFirst().orElse(null);

        if (exists != null) return true;

        Object[] row = (Object[]) em.createNativeQuery("""
            SELECT d.buyer_idx, d.seller_idx2
              FROM chat_rooms r
              JOIN deal d ON d.d_idx = r.d_idx
             WHERE r.ch_idx = :roomId
             LIMIT 1
        """).setParameter("roomId", roomId)
                .getResultStream().findFirst().orElse(null);

        if (row == null) return false;
        Long buyer  = row[0] == null ? null : ((Number) row[0]).longValue();
        Long seller = row[1] == null ? null : ((Number) row[1]).longValue();
        return (buyer != null && buyer.equals(userId)) || (seller != null && seller.equals(userId));
    }

    /**
     * deal(d_idx) 매핑 우선순위:
     *  1) 동일 상품(pd_idx) + 동일 판매자(seller_idx2) + 동일 구매자(buyer_idx)
     *  2) 동일 상품(pd_idx) + 동일 판매자(seller_idx2) (buyer 미정인 deal 존재 시)
     *  3) 동일 상품(pd_idx) (상품당 1건 unique 제약이 있으므로 이것만으로도 대부분 1건)
     */
    private Long findDealId(Long productId, Long sellerId, Long buyerId) {
        // 1) pd + seller + buyer 정확 일치
        Object r1 = em.createNativeQuery("""
            SELECT d_idx FROM deal
             WHERE pd_idx = :pd AND seller_idx2 = :seller AND buyer_idx = :buyer
             LIMIT 1
        """).setParameter("pd", productId)
                .setParameter("seller", sellerId)
                .setParameter("buyer", buyerId)
                .getResultStream().findFirst().orElse(null);
        if (r1 != null) return ((Number) r1).longValue();

        // 2) pd + seller 일치 (buyer 미정 케이스 포함)
        Object r2 = em.createNativeQuery("""
            SELECT d_idx FROM deal
             WHERE pd_idx = :pd AND seller_idx2 = :seller
             LIMIT 1
        """).setParameter("pd", productId)
                .setParameter("seller", sellerId)
                .getResultStream().findFirst().orElse(null);
        if (r2 != null) return ((Number) r2).longValue();

        // 3) pd 일치 (deal.pd_idx UNIQUE 라서 1건이면 이것으로 연결)
        Object r3 = em.createNativeQuery("""
            SELECT d_idx FROM deal
             WHERE pd_idx = :pd
             LIMIT 1
        """).setParameter("pd", productId)
                .getResultStream().findFirst().orElse(null);
        if (r3 != null) return ((Number) r3).longValue();

        return null;
    }

    @Transactional
    public OpenChatRoomRes openOrGetRoom(OpenChatRoomReq req, Long buyerId) {
        if (buyerId == null || req.getSellerId() == null)
            throw new IllegalArgumentException("buyerId, sellerId는 필수입니다.");
        if (buyerId.equals(req.getSellerId()))
            throw new IllegalArgumentException("buyerId와 sellerId는 달라야 합니다.");
        if (req.getProductId() == null)
            throw new IllegalArgumentException("productId는 필수입니다.");

        String identifier = buildIdentifier(buyerId, req.getSellerId(), req.getProductId());

        // 미리 deal 탐색
        Long foundDealId = findDealId(req.getProductId(), req.getSellerId(), buyerId);
        DealEntity dealRef = (foundDealId != null) ? em.getReference(DealEntity.class, foundDealId) : null;

        return chatRoomRepository.findByChIdentifier(identifier)
                .map(room -> {
                    // 기존 방이면 updated만 갱신하고, d_idx 비어있으면 채워줌
                    room.setChUpdated(LocalDateTime.now());
                    if (room.getDeal() == null && dealRef != null) {
                        room.setDeal(dealRef);
                        chatRoomRepository.save(room);
                    }
                    return OpenChatRoomRes.builder()
                            .roomId(room.getChIdx())
                            .created(false)
                            .identifier(identifier)
                            .dealId((room.getDeal() != null) ? room.getDeal().getDIdx() : foundDealId)
                            .build();
                })
                .orElseGet(() -> {
                    ProductEntity productRef = em.getReference(ProductEntity.class, req.getProductId());

                    ChatRoomEntity saved = chatRoomRepository.save(
                            ChatRoomEntity.builder()
                                    .product(productRef)
                                    .deal(dealRef) // ✅ 새로 만들 때 d_idx 연결
                                    .chIdentifier(identifier)
                                    .chCreated(LocalDateTime.now())
                                    .chUpdated(LocalDateTime.now())
                                    .build()
                    );

                    final Long newRoomId = saved.getChIdx();

                    // 참여자 읽음 포인터 초기화
                    chatRoomMapper.upsertRead(newRoomId, buyerId);
                    chatRoomMapper.upsertRead(newRoomId, req.getSellerId());

                    // 시스템 환영 메시지
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("chIdx", newRoomId);
                    msg.put("content", WELCOME);
                    chatRoomMapper.insertSystemMessage(msg);

                    return OpenChatRoomRes.builder()
                            .roomId(newRoomId)
                            .created(true)
                            .identifier(identifier)
                            .dealId(foundDealId) // 응답에 현재 연결된 dealId 알려줌(없으면 null)
                            .build();
                });
    }

    private String buildIdentifier(Long buyerId, Long sellerId, Long productId) {
        Long a = Math.min(buyerId, sellerId);
        Long b = Math.max(buyerId, sellerId);
        return a + ":" + b + ":" + productId;
    }
}
