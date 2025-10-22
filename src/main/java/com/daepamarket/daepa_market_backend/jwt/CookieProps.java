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

    //쿠키를 허용할 도메인
    private String domain;

    //HTTP 환경일 때 true
    private boolean secure;

    //SameSite (배포단계에서는 None으로 바꾸기)
    private String samesite = "Lax";

    //쿠키 기본 경로
    private String path = "/";

    //기본 쿠키 만료시간
    private long defaultcookie = 3600;
}
