package com.daepamarket.daepa_market_backend.config;

import com.daepamarket.daepa_market_backend.jwt.oauth.CustomOAuth2UserService;
import com.daepamarket.daepa_market_backend.jwt.oauth.OAuth2SuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * yml:
     * custom:
     *   cors:
     *     allowed-origins: http://localhost:3000
     */
    @Value("${custom.cors.allowed-origins:*}")
    private String allowedOriginsProp;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            // ✅ 존재하지 않아도 구동되도록 Optional 주입
            ObjectProvider<CustomOAuth2UserService> customOAuth2UserService,
            ObjectProvider<OAuth2SuccessHandler> oAuth2SuccessHandler,
            ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository
    ) throws Exception {

        // --- CORS / CSRF ---
        http.cors(Customizer.withDefaults());
        http.csrf(AbstractHttpConfigurer::disable);

        // --- 인가 규칙 ---
        http.authorizeHttpRequests(auth -> auth
                // 프리플라이트 허용
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 정적/헬스/에러/업로드
                .requestMatchers(
                        "/",
                        "/favicon.ico",
                        "/error",
                        "/actuator/health",
                        "/uploads/**",
                        "/images/**",
                        "/ws-stomp/**"
                ).permitAll()
                // API 전체(필요 시 .authenticated()로 바꿔도 됨)
                .requestMatchers("/api/**").permitAll()
                // OAuth2 엔드포인트
                .requestMatchers("/oauth2/**", "/login/**").permitAll()
                // 나머지
                .anyRequest().permitAll()
        );

        // --- OAuth2 (빈이 없으면 자동 비활성화) ---
        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .userInfoEndpoint(userInfo ->
                            userInfo.userService(customOAuth2UserService.getIfAvailable())
                    )
                    .successHandler(oAuth2SuccessHandler.getIfAvailable())
            );
        } else {
            // 빈이 없을 때는 명시적으로 비활성화 (구동 실패 방지)
            http.oauth2Login(AbstractHttpConfigurer::disable);
        }

        // 베이직/폼로그인은 불필요하니 비활성화
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 쉼표로 구분된 도메인 문자열을 목록으로 변환
        List<String> origins = Arrays.stream(allowedOriginsProp.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        CorsConfiguration cfg = new CorsConfiguration();
        // 와일드카드(*)만 들어오면 패턴 허용으로 처리
        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            cfg.addAllowedOriginPattern("*");
        } else {
            origins.forEach(cfg::addAllowedOrigin);
        }
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setExposedHeaders(List.of("Authorization", "Set-Cookie", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
