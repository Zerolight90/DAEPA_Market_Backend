package com.daepamarket.daepa_market_backend.chat.controller;

import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtSupport {

    private final JwtProvider jwtProvider;

    /** HTTP 요청 쿠키 ACCESS_TOKEN → subject(userId) 파싱 */
    public Long resolveUserIdFromCookie(HttpServletRequest req) {
        String token = findCookie(req, "ACCESS_TOKEN");
        return parseUserIdFromBearerOrRawToken(token);
    }

    /** STOMP 헤더/세션에서 userId 추출 (x-user-id 우선, 실패 시 쿠키 토큰) */
    public Long resolveUserIdFromHeaderOrCookie(SimpMessageHeaderAccessor accessor) {
        // 1) x-user-id (프론트가 CONNECT 시 헤더로 넣어줌)
        String fromHeader = accessor.getFirstNativeHeader("x-user-id");
        if (fromHeader != null && !fromHeader.isBlank()) {
            try { return Long.valueOf(fromHeader.trim()); } catch (Exception ignore) {}
        }
        // 2) 세션에 ACCESS_TOKEN 넣어둔 경우 (선택)
        Object tokenAttr = accessor.getSessionAttributes() == null ? null : accessor.getSessionAttributes().get("ACCESS_TOKEN");
        if (tokenAttr instanceof String tokenStr) {
            Long uid = parseUserIdFromBearerOrRawToken(tokenStr);
            if (uid != null) return uid;
        }
        // 3) 실패
        return null;
    }

    /** "Bearer <token>" 또는 raw token 에서 subject(userId) 추출 */
    private Long parseUserIdFromBearerOrRawToken(String tokenRaw) {
        if (tokenRaw == null || tokenRaw.isBlank()) return null;
        String token = tokenRaw.trim();
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) token = token.substring(7).trim();
        try {
            String subject = jwtProvider.getUid(token);
            if (subject == null || subject.isBlank()) return null;
            return Long.valueOf(subject.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String findCookie(HttpServletRequest req, String name) {
        if (req == null) return null;
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
