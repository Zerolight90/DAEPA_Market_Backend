package com.daepamarket.daepa_market_backend.chat.service;

import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper; // ✅ [추가] MyBatis 매퍼 주입
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
import java.util.HashMap; // ✅ [추가]
import java.util.List;
import java.util.Map;   // ✅ [추가]

@Service
@RequiredArgsConstructor
public class RoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMapper chatRoomMapper; // ✅ [추가] MyBatis 매퍼 주입

    @PersistenceContext
    private EntityManager em;

    // ✅ [추가] 환영 메시지 상수
    private static final String WELCOME =
            "안전한 거래를 위해 개인정보을 포함한 내용은 채팅은 삼가해주세요";

    // ✅ (1) MyBatis: 채팅방 목록 조회 (수정된 listRooms 쿼리 사용)
    @Transactional(readOnly = true)
    public List<ChatRoomListDto> listRooms(Long userIdOrNull) {
        return chatRoomMapper.listRooms(userIdOrNull);
    }

    // ✅ (2) JPA: WebSocket 보안용 참가자 확인 (이 로직은 chat_reads 테이블을 보므로 OK)
    @Transactional(readOnly = true)
    public boolean isParticipant(Long roomId, Long userId) {
        // chat_reads에 레코드가 있으면 참가자
        Object exists = em.createNativeQuery("""
        SELECT 1 FROM chat_reads
         WHERE ch_idx = :roomId AND cread_reader = :userId
         LIMIT 1
        """).setParameter("roomId", roomId)
                .setParameter("userId", userId)
                .getResultStream().findFirst().orElse(null);

        if (exists != null) return true;

        // (백업) deal 연결된 경우 기존 로직 (이 부분은 유지하거나 제거해도 됩니다)
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


    // ✅ (3) JPA: 채팅방 생성 or 재사용
    @Transactional
    public OpenChatRoomRes openOrGetRoom(OpenChatRoomReq req, Long buyerId) {
        if (buyerId == null || req.getSellerId() == null)
            throw new IllegalArgumentException("buyerId, sellerId는 필수입니다.");
        if (buyerId.equals(req.getSellerId()))
            throw new IllegalArgumentException("buyerId와 sellerId는 달라야 합니다.");
        if (req.getProductId() == null)
            throw new IllegalArgumentException("productId는 필수입니다.");

        String identifier = buildIdentifier(buyerId, req.getSellerId(), req.getProductId());

        return chatRoomRepository.findByChIdentifier(identifier)
                .map(room -> {
                    // 이미 존재 → updated 시간 갱신 (JPA dirty checking으로 반영)
                    room.setChUpdated(LocalDateTime.now());

                    // ✅ [수정] 방이 이미 있어도 읽음 처리는 해주는 것이 좋습니다. (선택 사항)
                    // chatRoomMapper.upsertRead(room.getChIdx(), buyerId);
                    // chatRoomMapper.upsertRead(room.getChIdx(), req.getSellerId());

                    return OpenChatRoomRes.builder()
                            .roomId(room.getChIdx())
                            .created(false)
                            .identifier(identifier)
                            .build();
                })
                .orElseGet(() -> {
                    // 방이 없는 경우: 새로 생성 (JPA)
                    ProductEntity productRef = em.getReference(ProductEntity.class, req.getProductId());

                    ChatRoomEntity saved = chatRoomRepository.save(
                            ChatRoomEntity.builder()
                                    .product(productRef)
                                    .chIdentifier(identifier)
                                    .chCreated(LocalDateTime.now())
                                    .chUpdated(LocalDateTime.now())
                                    .build()
                    );

                    final Long newRoomId = saved.getChIdx();

                    // ✅ [추가] MyBatis 기능을 호출하여 누락된 로직(읽음, 환영) 수행
                    // 1. 읽음 포인터 두 명 생성
                    chatRoomMapper.upsertRead(newRoomId, buyerId);
                    chatRoomMapper.upsertRead(newRoomId, req.getSellerId());

                    // 2. SYSTEM 웰컴 메시지 삽입
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("chIdx", newRoomId);
                    msg.put("content", WELCOME);
                    chatRoomMapper.insertSystemMessage(msg);

                    return OpenChatRoomRes.builder()
                            .roomId(newRoomId) // 'saved' 객체에서 ID 사용
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