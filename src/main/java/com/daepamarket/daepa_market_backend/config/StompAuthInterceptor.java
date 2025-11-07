package com.daepamarket.daepa_market_backend.config;

import com.daepamarket.daepa_market_backend.chat.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final RoomService roomService;

    // 개발 중에는 true → 운영에서 false (혹은 프로필로 분기 추천)
    private static final boolean DEV_ALLOW_ALL = false;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = StompHeaderAccessor.wrap(message);
        StompCommand cmd = acc.getCommand();
        if (cmd == null) return message;

        // CONNECT에서는 인증/세션 세팅만 할 수도 있음 (필요 시 처리)
        if (cmd == StompCommand.SUBSCRIBE || cmd == StompCommand.SEND) {
            final String dest = acc.getDestination();
            if (dest != null && isChatDestination(dest)) {
                Long roomId = extractRoomId(dest);
                Long userId = extractUserId(acc);
                if (!DEV_ALLOW_ALL) {
                    // roomId/userId가 없으면 막기
                    if (roomId == null || userId == null) {
                        throw new IllegalStateException("Missing roomId or userId for chat access");
                    }
                    // 방 참여자만 허용
                    if (!roomService.isParticipant(roomId, userId)) {
                        throw new IllegalStateException("Unauthorized chat access: user=" + userId + ", room=" + roomId);
                    }
                }
            }
        }
        // 헤더/페이로드 수정 안 하면 원본 그대로 반환
        return message;
    }

    /** /sub/chats/{roomId} (구독) 또는 /app/chats/{roomId}/... (발행) 만 필터링 */
    private boolean isChatDestination(String dest) {
        return dest.startsWith("/sub/chats/") || dest.startsWith("/app/chats/");
    }

    /** 목적지에서 roomId 뽑기 */
    @Nullable
    private Long extractRoomId(String dest) {
        try {
            if (dest.startsWith("/sub/chats/")) {
                // 예: /sub/chats/9020
                String id = dest.substring("/sub/chats/".length());
                int slash = id.indexOf('/');
                return Long.valueOf(slash >= 0 ? id.substring(0, slash) : id);
            }
            if (dest.startsWith("/app/chats/")) {
                // 예: /app/chats/9020/send
                String rest = dest.substring("/app/chats/".length());
                int slash = rest.indexOf('/');
                String id = (slash >= 0) ? rest.substring(0, slash) : rest;
                return Long.valueOf(id);
            }
        } catch (Exception ignore) { /* fallthrough */ }
        return null;
    }

    /** x-user-id 헤더 → Principal → 세션 attr 순으로 추출 */
    @Nullable
    private Long extractUserId(StompHeaderAccessor acc) {
        // 1) 클라이언트 커스텀 헤더
        String h = acc.getFirstNativeHeader("x-user-id");
        if (h != null) {
            try { return Long.valueOf(h); } catch (Exception ignore) {}
        }
        // 2) Principal (WebSocketHandshakeInterceptor 등에서 세팅 가능)
        Principal p = acc.getUser();
        if (p != null) {
            try { return Long.valueOf(p.getName()); } catch (Exception ignore) {}
        }
        // 3) 세션 속성
        Object v = (acc.getSessionAttributes() != null) ? acc.getSessionAttributes().get("userId") : null;
        if (v instanceof Number) return ((Number) v).longValue();
        if (v != null) {
            try { return Long.valueOf(String.valueOf(v)); } catch (Exception ignore) {}
        }
        return null;
    }
}
