package com.daepamarket.daepa_market_backend.mapper;

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
}
