package com.daepamarket.daepa_market_backend.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "app.jwt")
@Component
@Getter
@Setter
public class JwtProps {
    //JWT 암호화 키
    private String secret;

    //JWT 발급자
    private String issuer;

    //Accesstoken 만료시간(분)
    private long accessExpMin;

    //Accesstoken 만료 시간(일)
    private long refreshExpDays;


}
