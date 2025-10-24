// src/main/java/com/daepamarket/daepa_market_backend/chat/ws/ChatWsController.java
package com.daepamarket.daepa_market_backend.chat.ws;

import com.daepamarket.daepa_market_backend.chat.controller.JwtSupport;
import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate broker;
    private final JwtSupport jwtSupport;

    @MessageMapping("/chats/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload ChatDto.SendMessageReq req,
                            SimpMessageHeaderAccessor accessor,
                            Principal principal) {

        Long senderId = sanitizeId(req.getSenderId());
        if (senderId == null) senderId = parseLongSafe(accessor.getFirstNativeHeader("x-user-id"));
        if (senderId == null && principal != null) senderId = parseLongSafe(principal.getName());
        if (senderId == null) senderId = jwtSupport.resolveUserIdFromHeaderOrCookie(accessor);

        if (senderId == null) {
            // 인증 실패 시 무시하거나 에러 처리 (여기선 무시)
            return;
        }

        req.setRoomId(roomId);
        req.setSenderId(senderId);

        var res = chatService.sendMessage(roomId, senderId, req.getText(), req.getImageUrl(), req.getTempId());

        broker.convertAndSend("/sub/chats/" + roomId, res);
    }

    @MessageMapping("/chats/{roomId}/read")
    public void markRead(@DestinationVariable Long roomId,
                         @Payload ChatDto.ReadEvent readEvent,
                         SimpMessageHeaderAccessor accessor,
                         Principal principal) {

        Long readerId = sanitizeId(readEvent.getReaderId());
        if (readerId == null) readerId = parseLongSafe(accessor.getFirstNativeHeader("x-user-id"));
        if (readerId == null && principal != null) readerId = parseLongSafe(principal.getName());
        if (readerId == null) readerId = jwtSupport.resolveUserIdFromHeaderOrCookie(accessor);

        if (readerId == null) return;

        Long appliedUpTo = chatService.markRead(roomId, readerId, readEvent.getLastSeenMessageId());

        ChatDto.ReadEvent ack = ChatDto.ReadEvent.builder()
                .type("READ")
                .roomId(roomId)
                .readerId(readerId)
                .lastSeenMessageId(appliedUpTo)
                .time(java.time.LocalDateTime.now())
                .build();

        broker.convertAndSend("/sub/chats/" + roomId, ack);
    }

    private static Long sanitizeId(Long v) { return v == null ? null : v; }

    private static Long parseLongSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.valueOf(s.trim()); } catch (Exception e) { return null; }
    }
}
