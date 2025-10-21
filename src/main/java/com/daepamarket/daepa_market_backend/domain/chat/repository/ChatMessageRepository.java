package com.daepamarket.daepa_market_backend.domain.chat.repository;

import com.daepamarket.daepa_market_backend.domain.chat.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    /*
     방의 최근 N개 메시지 (오래된 순 → 최신순)
     - 방 입장 시 최신 메시지 N개를 가져올 때 사용
     - ORDER BY cmIdx ASC 로 정렬 (시간순)
     - Pageable.ofSize(N) 으로 갯수 제한
     */
    @Query("""
           select m
             from ChatMessageEntity m
            where m.room.chIdx = :roomId
            order by m.cmIdx asc
           """)
    List<ChatMessageEntity> findLatestByRoomAsc(@Param("roomId") Long roomId, Pageable pageable);


    /*
      특정 작성자가 보낸 메시지들 (필요 시)
      - 작성자별 채팅 내역 조회용 (예: 디버깅 or 검색 기능)
      - ORDER BY cmIdx DESC (최신순)
     */
    List<ChatMessageEntity> findByRoom_ChIdxAndCmWriterOrderByCmIdxDesc(Long roomId, String writer);


    /*
     최신 1개 메시지 (리스트 미리보기용)
     채팅방 목록에서 마지막 메시지를 표시할 때 사용
     */
    Optional<ChatMessageEntity> findTop1ByRoom_ChIdxOrderByCmIdxDesc(Long roomId);


    /*
      가장 마지막 메시지 ID (읽음 처리용)
      - 읽음 상태 계산 시 사용 (max(cm_idx))
      “상대방의 lastSeenMessageId >= 내 메시지ID” 판정 시 활용
     */
    @Query("""
           select max(m.cmIdx)
             from ChatMessageEntity m
            where m.room.chIdx = :roomId
           """)
    Long findMaxMessageIdByRoom(@Param("roomId") Long roomId);
}
