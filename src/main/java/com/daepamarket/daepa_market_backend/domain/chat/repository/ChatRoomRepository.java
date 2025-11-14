package com.daepamarket.daepa_market_backend.domain.chat.repository;

import com.daepamarket.daepa_market_backend.domain.chat.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {


    Optional<ChatRoomEntity> findByChIdentifier(String chIdentifier);

    // ✅ deal.dIdx 로 방 찾기
    @Query("select r from ChatRoomEntity r where r.deal.dIdx = :dIdx")
    Optional<ChatRoomEntity> findByDealId(@Param("dIdx") Long dIdx);

    // ✅ product.pdIdx 기준 최신 방 (deal 미연결 구형데이터 fallback)
    @Query("select r from ChatRoomEntity r where r.product.pdIdx = :pdIdx order by r.chUpdated desc")
    Optional<ChatRoomEntity> findLatestByProductId(@Param("pdIdx") Long pdIdx);

    // ✅ productId와 buyerId로 정확한 채팅방 찾기 (결제 알림용)
    Optional<ChatRoomEntity> findByProduct_PdIdxAndBuyer_UIdx(Long pdIdx, Long uIdx);
}
