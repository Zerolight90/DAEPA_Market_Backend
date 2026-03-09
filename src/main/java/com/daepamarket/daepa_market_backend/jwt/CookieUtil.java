package com.daepamarket.daepa_market_backend.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    public static final String ACCESS = "ACCESS_TOKEN";
    public static final String REFRESH = "REFRESH_TOKEN";

    private final CookieProps props;

    // --- 기존 방식 (호환성 유지용) ---
    public ResponseCookie accessCookie(String token, Duration maxAge) {
        return accessCookie(token, maxAge, true); // 기본은 영구 쿠키 유지
    }

    public ResponseCookie refreshCookie(String token, Duration maxAge) {
        return refreshCookie(token, maxAge, true);
    }

    // 🚨 [신규 추가] 자동 로그인 여부(isAutoLogin)에 따라 수명을 조절하는 진짜 메서드!
    public ResponseCookie accessCookie(String token, Duration maxAge, boolean isAutoLogin) {
        // 자동 로그인 안 하면 수명을 -1(세션 쿠키)로 만들어 브라우저 종료 시 증발시킵니다.
        return base(ACCESS, token, isAutoLogin ? maxAge : Duration.ofSeconds(-1)).httpOnly(true).build();
    }

    public ResponseCookie refreshCookie(String token, Duration maxAge, boolean isAutoLogin) {
        // 자동 로그인 안 하면 수명을 -1(세션 쿠키)로 만들어 브라우저 종료 시 증발시킵니다.
        return base(REFRESH, token, isAutoLogin ? maxAge : Duration.ofSeconds(-1)).httpOnly(true).build();
    }

    //로그아웃시 강제로 삭제할 쿠키 이름
    public ResponseCookie clear(String name) {
        return base(name, "", Duration.ZERO).maxAge(Duration.ZERO).build();
    }

    //쿠키의 기본 설정값
    private ResponseCookie.ResponseCookieBuilder base(String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path(props.getPath())
                .secure(props.isSecure())
                .sameSite(props.getSamesite())
                .maxAge(maxAge);
        // 도메인이 지정된 경우만 적용: localhost/127/배포 도메인 불일치로 쿠키가 안 남는 상황 방지
        if (props.getDomain() != null && !props.getDomain().isBlank()) {
            builder.domain(props.getDomain());
        }
        return builder;
    }

    // 요청(Request)의 쿠키에서 AccessToken만 쏙 빼오는 유틸 메서드
    public String getAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (ACCESS.equals(cookie.getName())) {
                    return cookie.getValue(); // 토큰 값 반환
                }
            }
        }
        return null; // AccessToken 쿠키가 없는 경우
    }
}