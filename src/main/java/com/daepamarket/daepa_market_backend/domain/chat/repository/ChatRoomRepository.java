package com.daepamarket.daepa_market_backend.domain.chat.repository;

import com.daepamarket.daepa_market_backend.domain.chat.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {

    // ch_idx (PK)로 조회는 기본 제공: findById(Long chIdx)

    // ch_identifier로 방 찾기 (상품 상세의 "채팅하기"에서 방 재사용할 때 유용)
    Optional<ChatRoomEntity> findByChIdentifier(String chIdentifier);

    // 거래/상품 기반으로 방 찾기 예시 (필요 시 활성화)
    // Optional<ChatRoomEntity> findByDeal_DIdx(Long dIdx);
    // List<ChatRoomEntity> findByProduct_PdIdx(Long pdIdx);
}
