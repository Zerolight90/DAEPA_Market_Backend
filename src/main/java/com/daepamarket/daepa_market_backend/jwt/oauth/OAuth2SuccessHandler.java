package com.daepamarket.daepa_market_backend.jwt.oauth;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProps;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;
    private final JwtProps jwtProps;

    @Value("${app.front-url}")
    private String frontUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();   

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        log.info("OAuth2 attrs from {} = {}", provider, attrs);

        String email = null;
        String name = null;
        String nickname = null;
        String providerId = null;

        if ("naver".equalsIgnoreCase(provider)) {
            Object respObj = attrs.get("response");
            Map<String, Object> naverUser;
            if (respObj instanceof Map<?, ?> respMap) {
                naverUser = (Map<String, Object>) respMap;
            } else {
                naverUser = attrs;
            }
            providerId = valueOf(naverUser.get("id"));
            email = str(naverUser.get("email"));
            name = str(naverUser.get("name"));
        } else if ("kakao".equalsIgnoreCase(provider)) {
            providerId = valueOf(attrs.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                nickname = profile != null ? str(profile.get("nickname")) : null;
                email = str(kakaoAccount.get("email"));
            }
        } else {
            providerId = valueOf(attrs.get("id"));
            email = str(attrs.get("email"));
            name = str(attrs.get("name"));
        }

        String uid = StringUtils.hasText(email) ? email : provider + "_" + providerId;
        log.info("provider={} uid={} email={}", provider, uid, email);

        UserEntity user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            user = new UserEntity();
            user.setUid(uid);
            user.setUname(name);
            user.setUJoinType(provider);
            user.setUStatus(9);                 
            user.setUDate(LocalDateTime.now());
            userRepository.save(user);
        } else {
            log.info("기존 소셜 유저 로그인 uid={} status={}", uid, user.getUStatus());
        }

       
        boolean isAutoLogin = false;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("oauth_auto_login".equals(c.getName())) {
                    isAutoLogin = "true".equals(c.getValue());
                     
                    ResponseCookie deleteAutoCookie = ResponseCookie.from("oauth_auto_login", "")
                            .path("/")
                            .maxAge(0)
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, deleteAutoCookie.toString());
                    break;
                }
            }
        }

        ResponseCookie clearAccess = cookieUtil.clear(CookieUtil.ACCESS);
        ResponseCookie clearRefresh = cookieUtil.clear(CookieUtil.REFRESH);
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        String accessToken = jwtProvider.createAccessToken(String.valueOf(user.getUIdx()), provider);
        String refreshToken = jwtProvider.createRefreshToken(String.valueOf(user.getUIdx()));

        user.setUrefreshToken(refreshToken);
        userRepository.save(user);

        
        ResponseCookie accessCookie = cookieUtil.accessCookie(accessToken, Duration.ofMinutes(jwtProps.getAccessExpMin()), isAutoLogin);
        ResponseCookie refreshCookie = cookieUtil.refreshCookie(refreshToken, Duration.ofDays(jwtProps.getRefreshExpDays()), isAutoLogin);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String redirectUrl = frontUrl + "/oauth/success"
                + "?provider=" + provider
                + "&status=" + user.getUStatus();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String str(Object o) { return o == null ? null : o.toString(); }
    private String valueOf(Object o) { return o == null ? null : o.toString(); }
}