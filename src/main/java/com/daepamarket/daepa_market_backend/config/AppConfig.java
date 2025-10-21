package com.daepamarket.daepa_market_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig { // 토스페이먼츠 API 호출을 위해서 필요한 컨피그

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
