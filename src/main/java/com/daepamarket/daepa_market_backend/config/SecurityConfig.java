package com.daepamarket.daepa_market_backend.config;

import com.daepamarket.daepa_market_backend.jwt.oauth.CustomOAuth2UserService;
import com.daepamarket.daepa_market_backend.jwt.oauth.OAuth2SuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ✅ .env의 CUSTOM_CORS_ALLOWED_ORIGINS를 직접 읽도록 수정하여 불확실성을 제거합니다.
    @Value("${CUSTOM_CORS_ALLOWED_ORIGINS:https://daepamarket.shop,https://www.daepamarket.shop}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CustomOAuth2UserService customOAuth2UserService, OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                // ✅ 세션을 사용하지 않고 JWT를 사용함을 명시 (중요)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/uploads/**", "/WEB-INF/views/**").permitAll()
                        .requestMatchers("/", "/ws-stomp/**", "/api/**", "/error", "/oauth2/**", "/login/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/api/oauth2/authorization"))
                        .redirectionEndpoint(redir -> redir
                                .baseUri("/api/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ 쉼표로 구분된 주소들을 리스트로 변환 (공백 제거 포함)
        List<String> origins = new ArrayList<>();
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            Arrays.stream(allowedOrigins.split(","))
                  .map(String::trim)
                  .filter(s -> !s.isEmpty())
                  .forEach(origins::add);
        }

        // 로컬 개발 환경 주소가 없으면 추가
        if (!origins.contains("http://localhost:3000")) {
            origins.add("http://localhost:3000");
        }

        config.setAllowCredentials(true); // ✅ 쿠키(토큰) 전송 허용 필수
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie", "Content-Disposition"));
        // ✅ 브라우저가 프리플라이트(OPTIONS) 요청 결과를 캐시할 시간 설정
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

//     @Bean
//     public PasswordEncoder passwordEncoder() {
//         return new BCryptPasswordEncoder();
//     }
    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        // NoOpPasswordEncoder는 암호화를 하지 않고 평문 그대로 비교하게 해줍니다.
        return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
    }
}