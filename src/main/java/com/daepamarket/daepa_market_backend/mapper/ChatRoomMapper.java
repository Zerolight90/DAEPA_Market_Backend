package com.daepamarket.daepa_market_backend.mapper;

import com.daepamarket.daepa_market_backend.common.dto.ChatRoomHeaderDto;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatRoomMapper {

    List<ChatRoomListDto> listRooms(@Param("userId") Long userId);

    int touchUpdated(@Param("roomId") Long roomId);

    Long findRoomIdByIdentifier(@Param("identifier") String identifier);

    int insertRoomIfAbsent(@Param("pdId") Long pdId,
                           @Param("identifier") String identifier);

    int upsertRead(@Param("roomId") Long roomId,
                   @Param("userId") Long userId);

    int isNewlyCreated(@Param("roomId") Long roomId);

    int insertSystemMessage(Map<String, Object> param);

    /** ✅ 방 나가기(해당 사용자 읽음행 삭제) */
    int deleteRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    /** ✅ 전체 안읽음 합계 (헤더 배지 용) */
    Integer countTotalUnread(@Param("userId") Long userId);

    // ⬇️ 헤더 정보
    ChatRoomHeaderDto selectRoomHeader(@Param("roomId") Long roomId,
                                       @Param("me") Long me);

    /** ✅ 방에서 현재 참여자 수(= chat_reads 행 수) */
    Integer countParticipants(@Param("roomId") Long roomId);

    boolean isParticipant(@Param("roomId") Long roomId, @Param("userId") Long userId);

    Long findDealId(@Param("productId") Long productId, @Param("sellerId") Long sellerId, @Param("buyerId") Long buyerId);
}
