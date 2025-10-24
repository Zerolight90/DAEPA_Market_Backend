package com.daepamarket.daepa_market_backend.chat.service;

import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.mapper.ChatMessageMapper;
import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageMapper messageMapper;
    private final ChatRoomMapper roomMapper;

    @Transactional
    public ChatDto.MessageRes sendMessage(Long roomId, Long senderId,
                                          String text, String imageUrl, String tempId) {

        final String type = (imageUrl != null && !imageUrl.isBlank()) ? "IMAGE" : "TEXT";
        final String content = text == null ? "" : text;

        Map<String, Object> param = new HashMap<>();
        param.put("chIdx", roomId);
        param.put("senderId", senderId);
        param.put("messageType", type);
        param.put("content", content);
        param.put("imageUrl", imageUrl);

        int inserted = messageMapper.insertMessage(param);
        if (inserted != 1) throw new IllegalStateException("insertMessage failed (roomId=" + roomId + ")");

        roomMapper.touchUpdated(roomId);

        Object pk = param.get("cmIdx");
        Long messageId = (pk instanceof Number) ? ((Number) pk).longValue() : null;

        return ChatDto.MessageRes.builder()
                .type(type)
                .messageId(messageId)
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .imageUrl(imageUrl)
                .time(LocalDateTime.now())
                .tempId(tempId)
                .build();
    }

    @Transactional
    public Long markRead(Long roomId, Long userId, Long upToOrNull) {
        if (upToOrNull == null) {
            messageMapper.upsertRead(roomId, userId);
        } else {
            messageMapper.upsertReadUpTo(roomId, userId, upToOrNull);
        }
        return upToOrNull;
    }
}
