package com.daepamarket.daepa_market_backend.jwt.oauth;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 🚨 1. 메서드 진입 여부 확인 로그 (이게 안 찍히면 카카오 통신(토큰) 자체의 문제입니다!)
        String provider = userRequest.getClientRegistration().getRegistrationId();
        log.info("🚀 [CustomOAuth2UserService] 진입 성공! 요청 Provider: {}", provider);

        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attrs = oAuth2User.getAttributes();
            log.info("✅ OAuth provider={} raw attrs={}", provider, attrs);

            String email = null;
            String name = null;
            String nickname = null;
            String providerId = null;
            String gender = null;
            String birth = null;
            String profileImage = null;

            // ---- 네이버 ----
            if ("naver".equalsIgnoreCase(provider)) {
                Map<String, Object> naver;
                Object respObj = attrs.get("response");
                if (respObj instanceof Map<?, ?> respMap) {
                    naver = (Map<String, Object>) respMap;
                } else {
                    naver = attrs;
                }

                providerId   = str(naver.get("id"));
                email        = str(naver.get("email"));
                name         = str(naver.get("name"));
                nickname     = str(naver.get("nickname"));
                gender       = str(naver.get("gender"));
                profileImage = str(naver.get("profile_image"));

                String birthyear = str(naver.get("birthyear"));
                String birthday  = str(naver.get("birthday"));
                birth = buildBirth(birthyear, birthday);

                // DB 제약조건 방어 (이름이 null일 경우 임시값 부여)
                name = (name == null || name.isBlank()) ? "네이버유저" : name;

                String uid = StringUtils.hasText(email) ? email : ("naver_" + providerId);
                upsertUser(uid, name, nickname, gender, birth, profileImage, "naver");

                Map<String, Object> attributesToReturn = (respObj instanceof Map<?, ?>) ? (Map<String, Object>) respObj : attrs;
                return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), attributesToReturn, "id");
            }
            // ---- 카카오 ----
            else if ("kakao".equalsIgnoreCase(provider)) {
                providerId = str(attrs.get("id"));
                Map<String, Object> kakaoAccount = safeMap(attrs.get("kakao_account"));
                
                if (kakaoAccount != null) {
                    email = str(kakaoAccount.get("email"));
                    Map<String, Object> profile = safeMap(kakaoAccount.get("profile"));
                    if (profile != null) {
                        nickname     = str(profile.get("nickname"));
                        profileImage = str(profile.get("profile_image_url"));
                    }
                }

                name = (name == null) ? nickname : name;
                // DB 제약조건 방어 (이름/닉네임이 모두 null일 경우 임시값 부여)
                name = (name == null || name.isBlank()) ? "카카오유저" : name;

                String uid = StringUtils.hasText(email) ? email : ("kakao_" + providerId);
                log.info("📝 카카오 DB 저장 직전 데이터: uid={}, name={}", uid, name);
                
                upsertUser(uid, name, nickname, null, null, profileImage, "kakao");
                log.info("🎉 DB 저장 성공! 카카오 로그인 완료!");

                return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), attrs, "id");
            }
            // ---- 그 외 ----
            else {
                providerId   = str(attrs.get("id"));
                email        = str(attrs.get("email"));
                name         = str(attrs.get("name"));
                nickname     = str(attrs.get("nickname"));
                profileImage = str(attrs.get("picture"));

                name = (name == null || name.isBlank()) ? "소셜유저" : name;

                String uid = StringUtils.hasText(email) ? email : (provider + "_" + providerId);
                upsertUser(uid, name, nickname, null, null, profileImage, provider);

                return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")), attrs, "id");
            }

        } catch (Exception e) {
            // 🚨 2. 어떤 에러가 나든 시큐리티가 숨기기 전에 우리가 먼저 터미널에 붉은 글씨로 찍어버립니다!
            log.error("💥 [치명적 에러] CustomOAuth2UserService 내부 로직 실패: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException(e.getMessage());
        }
    }

    // ============== 헬퍼들 ==============
    private void upsertUser(String uid, String name, String nickname, String gender, String birth, String profileImage, String joinType) {
        userRepository.findByUid(uid)
                .map(existing -> {
                    if (StringUtils.hasText(nickname))     existing.setUnickname(nickname);
                    if (StringUtils.hasText(gender))       existing.setUGender(gender);
                    if (StringUtils.hasText(birth))        existing.setUBirth(birth);
                    if (StringUtils.hasText(profileImage)) existing.setUProfile(profileImage);
                    if (StringUtils.hasText(joinType))     existing.setUJoinType(joinType);
                    if (existing.getUManner() == null)     existing.setUManner(20.0);
                    if (existing.getUWarn()   == null)     existing.setUWarn(0);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserEntity created = UserEntity.builder()
                            .uid(uid)
                            .uname(name)
                            .unickname(nickname)
                            .uGender(gender)
                            .uBirth(birth)
                            .uProfile(profileImage)
                            .uJoinType(joinType)
                            .uStatus(9)
                            .uWarn(0)
                            .uManner(20.0)
                            .uDate(LocalDateTime.now())
                            .build();
                    return userRepository.save(created);
                });
    }

    private String str(Object o) { return o == null ? null : o.toString(); }
    private Map<String, Object> safeMap(Object o) {
        try { return (Map<String, Object>) o; } catch (Exception e) { return null; }
    }
    private String buildBirth(String year, String birthday) {
        if (StringUtils.hasText(year) && StringUtils.hasText(birthday)) { return year + "-" + birthday; }
        return StringUtils.hasText(birthday) ? birthday : null;
    }
}