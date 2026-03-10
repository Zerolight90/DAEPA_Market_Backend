package com.daepamarket.daepa_market_backend.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityFailureLogger {

    // 🚨 시큐리티가 로그인 실패 처리할 때 발생하는 이벤트를 낚아채서 진짜 에러를 출력합니다!
    @EventListener
    public void onAuthFailure(AbstractAuthenticationFailureEvent event) {
        log.error("🚨 OAuth2 로그인 강제 종료 원인: {}", event.getException().getMessage());
        log.error("🚨 에러 상세 스택: ", event.getException());
    }
}