package com.daepamarket.daepa_market_backend.chat.service;

import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.mapper.ChatMessageMapper;
import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import com.daepamarket.daepa_market_backend.mapper.UserMapper;
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
    private final UserMapper userMapper;

    @Transactional
    public ChatDto.MessageRes sendMessage(Long roomId, Long senderId,
                                          String text, String imageUrl, String tempId) {
        final String type = (imageUrl != null && !imageUrl.isBlank()) ? "IMAGE" : "TEXT";
        final String content = text == null ? "" : text;

        String writer = userMapper.findLoginIdByIdx(senderId);
        if (writer == null || writer.isBlank()) writer = "unknown";

        Map<String, Object> param = new HashMap<>();
        param.put("chIdx", roomId);
        param.put("senderId", senderId);
        param.put("messageType", type);
        param.put("content", content);
        param.put("imageUrl", imageUrl);
        param.put("writer", writer);

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

    /**
     * 읽음 처리:
     * - upTo가 null이면 해당 방의 최신 메시지ID(MAX cm_idx)까지 읽음 처리
     * - upTo가 주어지면 기존 last_seen과 GREATEST로 끌어올림
     * - 항상 "적용된" last_seen_message_id 를 반환
     */
    @Transactional
    public Long markRead(Long roomId, Long userId, Long upToOrNull) {
        Long target;
        if (upToOrNull == null) {
            // 방의 최신 메시지까지로 target을 자동 계산
            Long maxId = messageMapper.selectMaxMessageId(roomId);
            if (maxId == null) maxId = 0L;
            messageMapper.upsertReadUpTo(roomId, userId, maxId);
            target = maxId;
        } else {
            messageMapper.upsertReadUpTo(roomId, userId, upToOrNull);
            target = upToOrNull;
        }
        // 실제 적용된 값(= DB상의 GREATEST 결과)을 다시 읽어 반환
        Long applied = messageMapper.selectLastSeen(roomId, userId);
        return applied == null ? 0L : applied;
    }
}
