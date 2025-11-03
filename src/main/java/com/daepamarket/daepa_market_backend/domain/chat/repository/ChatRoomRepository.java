package com.daepamarket.daepa_market_backend.domain.chat.repository;

import com.daepamarket.daepa_market_backend.domain.chat.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {


    Optional<ChatRoomEntity> findByChIdentifier(String chIdentifier);





}
