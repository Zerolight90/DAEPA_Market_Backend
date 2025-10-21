package com.daepamarket.daepa_market_backend.chat.ws;

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
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate broker;

    @MessageMapping("/chats/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload ChatDto.SendMessageReq req,
                            SimpMessageHeaderAccessor accessor,
                            Principal principal) {

        // 1) senderId 확보 (payload ≫ native headers ≫ session ≫ principal ≫ dev fallback)
        Long senderId = sanitizeId(req.getSenderId());
        if (senderId == null) senderId = parseLongSafe(accessor.getFirstNativeHeader("x-user-id"));
        if (senderId == null) senderId = extractFromSession(accessor);
        if (senderId == null && principal != null) senderId = parseLongSafe(principal.getName());
        if (senderId == null) senderId = 101L; // ⚠️ 개발용 fallback. 운영에선 제거/인증 연동.

        req.setRoomId(roomId);
        req.setSenderId(senderId);

        // 로깅
        System.out.println("[WS/SEND] room=" + roomId + " sender=" + senderId +
                " text=" + (req.getText() == null ? "" : req.getText()));

        // 2) 저장
        var res = chatService.sendMessage(roomId, senderId, req.getText(), req.getImageUrl(), req.getTempId());

        // 3) 브로드캐스트 (프론트 구독 prefix와 반드시 일치: /sub)
        broker.convertAndSend("/sub/chats/" + roomId, res);
    }

    @MessageMapping("/chats/{roomId}/read")
    public void markRead(@DestinationVariable Long roomId,
                         @Payload ChatDto.ReadEvent readEvent,
                         SimpMessageHeaderAccessor accessor,
                         Principal principal) {

        Long readerId = sanitizeId(readEvent.getReaderId());
        if (readerId == null) readerId = parseLongSafe(accessor.getFirstNativeHeader("x-user-id"));
        if (readerId == null) readerId = extractFromSession(accessor);
        if (readerId == null && principal != null) readerId = parseLongSafe(principal.getName());
        if (readerId == null) readerId = 101L; // 개발용

        chatService.markReadToLatest(roomId, readerId);
    }

    // ---- helpers ----
    private static Long extractFromSession(SimpMessageHeaderAccessor accessor) {
        if (accessor.getSessionAttributes() == null) return null;
        Object v = accessor.getSessionAttributes().get("userId");
        return v == null ? null : parseLongSafe(String.valueOf(v));
    }

    private static Long sanitizeId(Long v) { return v == null ? null : v; }

    private static Long parseLongSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.valueOf(s.trim()); } catch (Exception e) { return null; }
    }
}
