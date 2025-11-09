package com.daepamarket.daepa_market_backend.chat.service;

import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.domain.chat.ChatMessageEntity;
import com.daepamarket.daepa_market_backend.mapper.ChatMessageMapper;
import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import com.daepamarket.daepa_market_backend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final ChatMessageMapper msgMapper;
    private final SimpMessagingTemplate broker;


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

    // ------------------------------------------------------
    // ✅ 공통: SYSTEM 메시지 기록 + 방 갱신 + STOMP 브로드캐스트
    // ------------------------------------------------------
    @Transactional
    public ChatDto.MessageRes sendSystemMessage(Long roomId, String text) {
        Map<String, Object> p = new HashMap<>();
        p.put("chIdx", roomId);
        p.put("content", text);
        roomMapper.insertSystemMessage(p);

        roomMapper.touchUpdated(roomId);

        Long lastId = messageMapper.selectMaxMessageId(roomId);
        if (lastId == null) lastId = 0L;

        ChatDto.MessageRes sys = ChatDto.MessageRes.builder()
                .type("SYSTEM")
                .messageId(lastId)
                .roomId(roomId)
                .senderId(null)
                .content(text)
                .imageUrl(null)
                .time(LocalDateTime.now())
                .build();

        broker.convertAndSend("/sub/chats/" + roomId, sys);
        return sys;
    }

    // ------------------------------------------------------
    // ✅ 시나리오 1: 구매자 입금 완료 알림 (💸)
    // ------------------------------------------------------
    @Transactional
    public ChatDto.MessageRes sendBuyerDeposited(Long roomId, Long buyerId,
                                                 String productTitle, Long price) {
        String buyerName = userMapper.findDisplayNameByIdx(buyerId);
        if (buyerName == null || buyerName.isBlank()) buyerName = "구매자";

        String text = String.format(
                "💸 %s님이 \"%s\" (%s원)을 입금했어요! 판매 확정을 눌러주세요.",
                buyerName,
                safeProductTitle(productTitle),
                formatPrice(price)
        );
        return sendSystemMessage(roomId, text);
    }

    // ------------------------------------------------------
    // ✅ 시나리오 2: 판매자 판매 확정 알림 (📦) #d_sell 이 1되면 메시지 가게끔
    // ------------------------------------------------------
    @Transactional
    public ChatDto.MessageRes sendSellerConfirmed(Long roomId, Long sellerId,
                                                  String productTitle, Long price) {
        String text = String.format(
                "📦 \"%s\" (%s원) 거래가 판매 확정되었습니다.\n반드시 물건을 인수 후 구매확정을 눌러주세요.",
                safeProductTitle(productTitle),
                formatPrice(price)
        );
        return sendSystemMessage(roomId, text);
    }

    // ------------------------------------------------------
    // ✅ 포맷 헬퍼
    // ------------------------------------------------------
    private String formatPrice(Long price) {
        if (price == null) return "-";
        return String.format("%,d", price);
    }

    private String safeProductTitle(String t) {
        if (t == null || t.isBlank()) return "상품";
        // 따옴표나 줄바꿈 제거
        return t.replace("\"", "").replace("\n", " ").trim();
    }
}

