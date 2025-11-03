package com.daepamarket.daepa_market_backend.jwt.oauth;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAttributes {

    private Map<String, Object> attributes;  // 전체 응답 데이터
    private String nameAttributeKey;         // 식별자 키 (보통 "id")

    // 네이버에서 가져올 데이터들
    private String email;
    private String name;
    private String nickname;
    private String gender;
    private String birth;
    private String birthyear;
    private String mobile;
    private String profileImage;

    private String joinType;

    /**
     * 소셜 구분 (네이버, 구글, 카카오 등)
     */
    public static OAuthAttributes of(String registrationId,
                                     String userNameAttributeName,
                                     Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        return ofDefault(userNameAttributeName, attributes, registrationId);
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(String userNameAttributeName,
                                           Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        log.info("Naver OAuth response = {}", response);

        return OAuthAttributes.builder()
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .email((String) response.get("email"))
                .name((String) response.get("name"))
                .nickname((String) response.get("nickname"))
                .gender((String) response.get("gender"))
                .birth((String) response.get("birthday"))
                .birthyear((String) response.get("birthyear"))
                .mobile((String) response.get("mobile"))
                .profileImage((String) response.get("profile_image"))
                .joinType("naver") // ✅ 여기!
                .build();
    }

    // 혹시 다른 provider(구글 등)가 들어와도 터지지 않게
    private static OAuthAttributes ofDefault(String userNameAttributeName,
                                             Map<String, Object> attributes,
                                             String registrationId) {
        return OAuthAttributes.builder()
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .email((String) attributes.get("email"))
                .name((String) attributes.getOrDefault("name", ""))
                .nickname((String) attributes.getOrDefault("nickname", ""))
                .joinType(registrationId)
                .build();
    }

    /**
     * OAuth 유저를 UserEntity로 변환
     */
    public UserEntity toEntity() {
        return UserEntity.builder()
                .uid(email)                    // 이메일을 아이디로 사용
                .uname(name)                   // 이름
                .unickname(nickname)           // 닉네임
                .uGender(gender)
                .uBirth(birthyear + "-" + birth) // 생일 조합 (YYYY-MM-DD)
                .uphone(mobile)
                .uProfile(profileImage)
                .uJoinType(joinType)           // ✅ "naver"
                .uStatus(1)                    // 기본 활성 상태
                .uWarn(0)
                .uManner(20.0)
                .uDate(LocalDateTime.now())
                .build();
    }
}
