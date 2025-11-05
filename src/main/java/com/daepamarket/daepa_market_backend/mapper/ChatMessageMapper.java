package com.daepamarket.daepa_market_backend.mapper;

import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.domain.chat.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper {

    List<ChatDto.MessageRes> findMessages(@Param("roomId") Long roomId,
                                          @Param("before") Long before,
                                          @Param("size") int size);

    int insertMessage(Map<String, Object> param);

    int upsertReadUpTo(@Param("roomId") Long roomId,
                       @Param("userId") Long userId,
                       @Param("upTo") Long upTo);

    int countUnread(@Param("roomId") Long roomId, @Param("userId") Long userId);

    List<ChatDto.MessageRes> findMessagesAfter(@Param("roomId") Long roomId,
                                               @Param("after") Long after,
                                               @Param("size") int size);

    Long selectMaxMessageId(@Param("roomId") Long roomId);

    Long selectLastSeen(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
