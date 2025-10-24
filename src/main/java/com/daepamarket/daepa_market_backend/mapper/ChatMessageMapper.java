package com.daepamarket.daepa_market_backend.mapper;

import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper {

    List<ChatDto.MessageRes> findMessages(@Param("roomId") Long roomId,
                                          @Param("before") Long before,
                                          @Param("size") int size);

    ChatDto.MessageRes findOneById(@Param("id") Long id);

    int insertMessage(Map<String, Object> param);

    int upsertRead(@Param("roomId") Long roomId, @Param("userId") Long userId);

    int upsertReadUpTo(@Param("roomId") Long roomId,
                       @Param("userId") Long userId,
                       @Param("upTo") Long upTo);

    int countUnread(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
