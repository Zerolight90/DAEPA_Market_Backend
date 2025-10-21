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

        // MyBatis 파라미터 (키 이름은 XML과 1:1 매칭)
        Map<String, Object> param = new HashMap<>();
        param.put("chIdx", roomId);
        param.put("senderId", senderId);
        param.put("messageType", type);
        param.put("content", content);
        param.put("imageUrl", imageUrl);

        int inserted = messageMapper.insertMessage(param);
        if (inserted != 1) {
            throw new IllegalStateException("insertMessage failed (roomId=" + roomId + ")");
        }

        // 방 updated 갱신
        roomMapper.touchUpdated(roomId);

        // 생성 PK 회수 (Long/Integer 모두 대응)
        Object pk = param.get("cmIdx");
        Long messageId = (pk instanceof Number) ? ((Number) pk).longValue() : null;

        return ChatDto.MessageRes.builder()
                .type(type)
                .messageId(messageId)
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .imageUrl(imageUrl)
                .time(LocalDateTime.now()) // DB NOW()와 약간 차이 있어도 OK
                .tempId(tempId)
                .build();
    }

    @Transactional
    public void markReadToLatest(Long roomId, Long userId) {
        messageMapper.upsertRead(roomId, userId);
    }
}
