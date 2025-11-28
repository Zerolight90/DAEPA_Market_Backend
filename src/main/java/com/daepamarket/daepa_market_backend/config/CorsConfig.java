package com.daepamarket.daepa_market_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${custom.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 설정 파일에서 읽어온 허용할 오리진 목록
        List<String> origins = new ArrayList<>(Arrays.asList(allowedOrigins.split(",")));

        // 로컬 개발 환경(http://localhost:3000)을 항상 허용 목록에 추가 (중복 방지)
        if (!origins.contains("http://localhost:3000")) {
            origins.add("http://localhost:3000");
        }

        config.setAllowCredentials(true);
        config.setAllowedOrigins(origins); // 수정된 오리진 목록 사용
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
