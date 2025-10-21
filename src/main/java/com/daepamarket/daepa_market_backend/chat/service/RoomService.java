package com.daepamarket.daepa_market_backend.chat.service;

import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomListDto;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomReq;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomRes;
import com.daepamarket.daepa_market_backend.domain.chat.ChatRoomEntity;
import com.daepamarket.daepa_market_backend.domain.chat.repository.ChatRoomRepository;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper;

    @PersistenceContext
    private EntityManager em;

    // ✅ (1) MyBatis: 채팅방 목록 조회
    @Transactional(readOnly = true)
    public List<ChatRoomListDto> listRooms(Long userIdOrNull) {
        return chatRoomMapper.listRooms(userIdOrNull);
    }

    // ✅ (2) JPA: WebSocket 보안용 참가자 확인
    @Transactional(readOnly = true)
    public boolean isParticipant(Long roomId, Long userId) {
        Object[] row = (Object[]) em.createNativeQuery("""
            SELECT d.buyer_idx, d.seller_idx2
              FROM chat_rooms r
              JOIN deal d ON d.d_idx = r.d_idx
             WHERE r.ch_idx = :roomId
             LIMIT 1
        """).setParameter("roomId", roomId)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (row == null) return false;

        Long buyer  = row[0] == null ? null : ((Number) row[0]).longValue();
        Long seller = row[1] == null ? null : ((Number) row[1]).longValue();

        boolean ok = (buyer != null && buyer.equals(userId)) ||
                (seller != null && seller.equals(userId));

        System.out.println("[WS CHECK] room=" + roomId +
                " buyer=" + buyer + " seller=" + seller +
                " user=" + userId + " → allowed=" + ok);
        return ok;
    }

    // ✅ (3) JPA: 채팅방 생성 or 재사용
    @Transactional
    public OpenChatRoomRes openOrGetRoom(OpenChatRoomReq req) {
        if (req.getBuyerId() == null || req.getSellerId() == null)
            throw new IllegalArgumentException("buyerId, sellerId는 필수입니다.");
        if (req.getBuyerId().equals(req.getSellerId()))
            throw new IllegalArgumentException("buyerId와 sellerId는 달라야 합니다.");
        if (req.getProductId() == null)
            throw new IllegalArgumentException("productId는 필수입니다.");

        String identifier = buildIdentifier(req.getBuyerId(), req.getSellerId(), req.getProductId());

        return chatRoomRepository.findByChIdentifier(identifier)
                .map(room -> {
                    // 이미 존재 → updated 시간 갱신
                    room.setChUpdated(LocalDateTime.now());
                    return OpenChatRoomRes.builder()
                            .roomId(room.getChIdx())
                            .created(false)
                            .identifier(identifier)
                            .build();
                })
                .orElseGet(() -> {
                    ProductEntity productRef = em.getReference(ProductEntity.class, req.getProductId());

                    ChatRoomEntity.ChatRoomEntityBuilder builder = ChatRoomEntity.builder()
                            .product(productRef)
                            .chIdentifier(identifier)
                            .chCreated(LocalDateTime.now())
                            .chUpdated(LocalDateTime.now());

                    // 💡 dealId 있을 때만 세팅
                    if (req.getDealId() != null) {
                        DealEntity dealRef = em.getReference(DealEntity.class, req.getDealId());
                        builder.deal(dealRef);
                    }

                    ChatRoomEntity saved = chatRoomRepository.save(builder.build());

                    return OpenChatRoomRes.builder()
                            .roomId(saved.getChIdx())
                            .created(true)
                            .identifier(identifier)
                            .build();
                });
    }


    private String buildIdentifier(Long buyerId, Long sellerId, Long productId) {
        Long a = Math.min(buyerId, sellerId);
        Long b = Math.max(buyerId, sellerId);
        return a + ":" + b + ":" + productId;
    }
}
