package com.daepamarket.daepa_market_backend.domain.chatread.repository;

import com.daepamarket.daepa_market_backend.domain.chatread.ChatReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatReadRepository extends JpaRepository<ChatReadEntity, Long> {

    /** 방 + 사용자 기준으로 읽음 상태 조회 */
    Optional<ChatReadEntity> findByRoomIdAndReaderId(Long roomId, Long readerId);
}
