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
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        // 0) 원본 속성 로드
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "naver" | "kakao" | ...
        Map<String, Object> attrs = oAuth2User.getAttributes();

        log.info("✅ OAuth provider={} raw attrs={}", provider, attrs);

        // 표준화용 변수
        String email = null;
        String name = null;
        String nickname = null;
        String providerId = null;
        String gender = null;
        String birth = null;
        String profileImage = null;

        // ---- 네이버 ---------------------------------------------------------
        if ("naver".equalsIgnoreCase(provider)) {
            // 어떤 환경: { response: {...} }, 어떤 환경: 평평(flat)
            Map<String, Object> naver = null;
            Object respObj = attrs.get("response");
            if (respObj instanceof Map<?, ?> respMap) {
                // 중첩된 형태
                // SuccessHandler가 attrs.get("response")를 먼저 시도하니 이것도 호환됨
                naver = (Map<String, Object>) respMap;
            } else {
                // 평평한 형태
                naver = attrs;
            }

            providerId   = str(naver.get("id"));
            email        = str(naver.get("email"));
            name         = str(naver.get("name"));
            nickname     = str(naver.get("nickname"));
            gender       = str(naver.get("gender"));
            profileImage = str(naver.get("profile_image"));

            // birthyear + birthday(월-일) → YYYY-MM-DD 형태로 합치기
            String birthyear = str(naver.get("birthyear"));
            String birthday  = str(naver.get("birthday")); // MM-DD
            birth = buildBirth(birthyear, birthday);

            // ---- DB upsert ----
            String uid = StringUtils.hasText(email) ? email : ("naver_" + providerId);
            upsertUser(uid, name, nickname, gender, birth, profileImage, "naver");

            // 네이버는 SuccessHandler가 "response"를 먼저 보므로
            // 여기서는 "response" 맵(또는 평평 케이스면 attrs 자체)을 attributes로 돌려주고,
            // nameAttributeKey는 "id"로 지정
            Map<String, Object> attributesToReturn = (respObj instanceof Map<?, ?>)
                    ? (Map<String, Object>) respObj   // 진짜 response만
                    : attrs;                           // 평평한 케이스면 그대로
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attributesToReturn,
                    "id"
            );
        }

        // ---- 카카오 ---------------------------------------------------------
        else if ("kakao".equalsIgnoreCase(provider)) {
            // 최상위: { id: ..., kakao_account: { email, profile: { nickname, profile_image_url } } }
            providerId = str(attrs.get("id"));

            Map<String, Object> kakaoAccount = safeMap(attrs.get("kakao_account"));
            if (kakaoAccount != null) {
                email = str(kakaoAccount.get("email"));

                Map<String, Object> profile = safeMap(kakaoAccount.get("profile"));
                if (profile != null) {
                    nickname     = str(profile.get("nickname"));
                    profileImage = str(profile.get("profile_image_url")); // 또는 "thumbnail_image_url"
                }
            }

            // 카카오는 name이 별도로 없을 때가 많으니 nickname으로 대체
            name = (name == null) ? nickname : name;

            // ---- DB upsert ----
            String uid = StringUtils.hasText(email) ? email : ("kakao_" + providerId);
            upsertUser(uid, name, nickname, null, null, profileImage, "kakao");

            // 카카오는 SuccessHandler가 최상위 attrs를 기준으로 파싱하므로
            // attrs 그대로 넘기고 nameAttributeKey="id"
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attrs,
                    "id"
            );
        }

        // ---- 그 외(구글 등) ---------------------------------------------------
        else {
            providerId   = str(attrs.get("id"));
            email        = str(attrs.get("email"));
            name         = str(attrs.get("name"));
            nickname     = str(attrs.get("nickname"));
            profileImage = str(attrs.get("picture"));

            String uid = StringUtils.hasText(email) ? email : (provider + "_" + providerId);
            upsertUser(uid, name, nickname, null, null, profileImage, provider);

            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    attrs,
                    "id"
            );
        }
    }

    // ============== 헬퍼들 ==============

    private void upsertUser(String uid,
                            String name,
                            String nickname,
                            String gender,
                            String birth,
                            String profileImage,
                            String joinType) {

        userRepository.findByUid(uid)
                .map(existing -> {
                    // 이미 있는 경우: 닉네임/프로필 정도만 부드럽게 동기화
                    if (StringUtils.hasText(nickname))     existing.setUnickname(nickname);
                    if (StringUtils.hasText(gender))       existing.setUGender(gender);
                    if (StringUtils.hasText(birth))        existing.setUBirth(birth);
                    if (StringUtils.hasText(profileImage)) existing.setUProfile(profileImage);
                    if (StringUtils.hasText(joinType))     existing.setUJoinType(joinType);

                    if (existing.getUManner() == null) existing.setUManner(20.0);
                    if (existing.getUWarn()   == null) existing.setUWarn(0);


                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    // 최초 로그인: status=9 (추가정보 필요)
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

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private Map<String, Object> safeMap(Object o) {
        try {
            return (Map<String, Object>) o;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildBirth(String year, String birthday) {
        // birthday == "MM-DD"
        if (StringUtils.hasText(year) && StringUtils.hasText(birthday)) {
            return year + "-" + birthday;
        }
        return StringUtils.hasText(birthday) ? birthday : null;
    }
}
