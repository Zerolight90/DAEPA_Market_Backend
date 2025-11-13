package com.daepamarket.daepa_market_backend.jwt.oauth;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    @Value("${app.front-url}")
    private String frontUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId();   // naver / kakao ...

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        // 👉 실제로 뭐가 왔는지 로그로 찍어보자
        log.info("✅ OAuth2 attrs from {} = {}", provider, attrs);

        // 공통으로 뽑을 값
        String email = null;
        String name = null;
        String nickname = null;
        String providerId = null;

        // =======================
        // 1) NAVER
        // =======================
        if ("naver".equalsIgnoreCase(provider)) {
            // 경우 A) { response : { ... } } 로 오는 경우
            Object respObj = attrs.get("response");

            Map<String, Object> naverUser;
            if (respObj instanceof Map<?, ?> respMap) {
                // 우리가 처음에 가정한 네이버 모양
                naverUser = (Map<String, Object>) respMap;
            } else {
                // 지금 네가 받은 건 이쪽이었어 → 평평한 형태라 여기로 온다
                naverUser = attrs;
            }

            providerId = valueOf(naverUser.get("id"));
            email = str(naverUser.get("email"));
            name = str(naverUser.get("name"));
        }
        // =======================
        // 2) KAKAO (혹시 나중에)
        // =======================
        else if ("kakao".equalsIgnoreCase(provider)) {
            providerId = valueOf(attrs.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attrs.get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                nickname = profile != null ? str(profile.get("nickname")) : null;
                email = str(kakaoAccount.get("email"));
            }
        }
        // =======================
        // 3) 그 외
        // =======================
        else {
            // 혹시 다른 provider 써도 안 터지게
            providerId = valueOf(attrs.get("id"));
            email = str(attrs.get("email"));
            name = str(attrs.get("name"));
        }

        // 이메일이 없으면 providerId로라도 uid를 만들어야 함
        String uid = StringUtils.hasText(email)
                ? email
                : provider + "_" + providerId;

        log.info("✅ provider={} uid={} email={}", provider, uid, email);

        // DB 조회
        UserEntity user = userRepository.findByUid(uid).orElse(null);
        if (user == null) {
            // 첫 소셜 로그인
            user = new UserEntity();
            user.setUid(uid);
            user.setUname(name);
            user.setUJoinType(provider);
            user.setUStatus(9);                 // 추가정보 입력 필요
            user.setUDate(LocalDateTime.now());
            userRepository.save(user);
//            log.info("🆕 신규 소셜 유저 생성 uid={} status=9", uid);
        } else {
            log.info("🟢 기존 소셜 유저 로그인 uid={} status={}", uid, user.getUStatus());
        }

        // 네가 만든 JwtProvider 그대로 사용
        String accessToken = jwtProvider.createAccessToken(uid, provider);
        String refreshToken = jwtProvider.createRefreshToken(uid);

        user.setUrefreshToken(refreshToken);
        userRepository.save(user);

        // 쿠키로 내려주기 (SameSite=None 설정을 위해 ResponseCookie 사용)
        ResponseCookie atCookie = ResponseCookie.from(CookieUtil.ACCESS, accessToken)
                .httpOnly(true)
                .path("/")
                .secure(true) // HTTPS 환경에서만 쿠키 전송
                .sameSite("None") // 다른 도메인 간의 요청에도 쿠키 전송 허용
                .build();
        response.addHeader("Set-Cookie", atCookie.toString());

        ResponseCookie rtCookie = ResponseCookie.from(CookieUtil.REFRESH, refreshToken)
                .httpOnly(true)
                .path("/")
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", rtCookie.toString());

        // 프론트로 리다이렉트
        // 여기서 status도 같이 넘겨줘서 프론트가 "추가정보 필요" 판단하게
        String redirectUrl = frontUrl + "/oauth/success"
                + "?provider=" + provider
                + "&accessToken=" + accessToken
                + "&refreshToken=" + refreshToken
                + "&status=" + user.getUStatus();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // ====== 작은 헬퍼들 ======
    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private String valueOf(Object o) {
        return o == null ? null : o.toString();
    }
}
