package com.daepamarket.daepa_market_backend.mapper;

import com.daepamarket.daepa_market_backend.common.dto.ChatRoomListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ChatRoomMapper {
    List<ChatRoomListDto> listRooms(@Param("userId") Long userId);
    int touchUpdated(@Param("roomId") Long roomId);
}
