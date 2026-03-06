package com.daepamarket.daepa_market_backend.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProps {

    // 쿠키를 허용할 도메인
    private String domain;

    // HTTPS 환경이면 true
    private boolean secure;

    // SameSite: 크로스 도메인 배포이므로 None
    private String samesite = "None";

    // 쿠키 기본 경로
    private String path = "/";

    // 기본 쿠키 만료시간
    private long defaultcookie = 3600;
}