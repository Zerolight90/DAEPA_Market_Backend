package com.daepamarket.daepa_market_backend.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    public static final String ACCESS = "ACCESS_TOKEN";
    public static final String REFRESH = "REFRESH_TOKEN";

    private final CookieProps props;

    //AccessToken 쿠키 생성
    public ResponseCookie accessCookie(String token, Duration maxAge) {
        return base(ACCESS, token, maxAge).httpOnly(true).build();
    }

    //RefreshToken 쿠키 생성
    public ResponseCookie refreshCookie(String token, Duration maxAge) {
        return base(REFRESH, token, maxAge).httpOnly(true).build();
    }

    //로그아웃시 강제로 삭제할 쿠키 이름
    public ResponseCookie clear(String name) {
        return base(name, "", Duration.ZERO).maxAge(Duration.ZERO).build();
    }

    //쿠키의 기본 설정값
    private ResponseCookie.ResponseCookieBuilder base(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
//                .domain(props.getDomain())
                .path(props.getPath())
                .secure(props.isSecure())
                .sameSite(props.getSamesite())
                .maxAge(maxAge);
    }
}
