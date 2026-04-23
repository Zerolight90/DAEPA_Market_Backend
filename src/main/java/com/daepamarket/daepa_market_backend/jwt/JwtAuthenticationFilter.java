package com.daepamarket.daepa_market_backend.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT Access Token 검증 필터.
 *
 * <p>요청마다 한 번만 실행되며(OncePerRequestFilter), HTTP-Only 쿠키 또는
 * Authorization 헤더에서 Access Token을 꺼내 검증합니다.
 * 토큰이 유효하면 SecurityContext에 인증 정보를 등록하여
 * 이후 컨트롤러에서 {@code @AuthenticationPrincipal}로 사용자 정보를 꺼낼 수 있습니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    /** 인증이 필요 없는 공개 경로 */
    private static final List<String> PERMIT_ALL_PATHS = List.of(
        "/api/user/signup",
        "/api/user/login",
        "/api/user/refresh",
        "/api/user/find-id",
        "/api/user/find-password",
        "/api/user/reset-password",
        "/api/mail/**",
        "/api/products/**",
        "/api/categories/**",
        "/api/notice/**",
        "/api/public/**",
        "/api/seller/**",
        "/api/banner/**",
        "/api/admin/banners/active",
        "/api/biz/**",
        "/api/faq/**",
        "/actuator/**",
        "/ws-stomp/**",
        "/api/oauth2/**",
        "/api/login/**"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return PERMIT_ALL_PATHS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && !jwtProvider.isExpired(token)) {
            String subject = jwtProvider.getUid(token);
            if (subject != null) {
                // role 클레임이 없는 경우 기본 USER 권한 부여
                String role = resolveRole(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT 인증 완료 - userId={}, role={}", subject, role);
            }
        }

        filterChain.doFilter(request, response);
    }

    /** HTTP-Only 쿠키 → Authorization 헤더 순서로 토큰 추출 */
    private String resolveToken(HttpServletRequest request) {
        // 1. Cookie 우선 탐색
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> CookieUtil.ACCESS.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        // 2. Authorization: Bearer <token> 헤더 fallback
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String resolveRole(String token) {
        try {
            var claims = jwtProvider.parse(token).getPayload();
            Object jointype = claims.get("jointype:");
            if (jointype instanceof String s && !s.isBlank()) {
                return s.toUpperCase();
            }
        } catch (Exception e) {
            log.debug("role 클레임 파싱 실패, 기본 USER 적용: {}", e.getMessage());
        }
        return "USER";
    }
}
