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

        // 1) 네이버에서 유저 정보 받아오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        // 2) 네이버면 응답이 {response={...}} 형태라서 안쪽을 한 번 더 꺼냄
        Map<String, Object> response;
        if ("naver".equals(registrationId)) {
            response = (Map<String, Object>) attrs.get("response");
            if (response == null) {
                // 네이버가 뭔가 이상하게 주면 여기서 한 번 막아줌
                throw new OAuth2AuthenticationException("Naver response is null");
            }
        } else {
            // 다른 provider일 때는 그냥 최상위 attrs 쓰자
            response = attrs;
        }

        // 3) DB에 저장/업데이트 (네가 앞에서 만든 로직 그대로 써도 됨)
        String email = (String) response.get("email");
        String nickname = (String) response.get("nickname");
        String name = (String) response.get("name");
        String gender = (String) response.get("gender");
        String birth = buildBirth((String) response.get("birthyear"), (String) response.get("birthday"));
        String profileImage = (String) response.get("profile_image");

        UserEntity user = userRepository.findByUid(email != null ? email : nickname)
                .map(existing -> {
                    existing.setUnickname(nickname);
                    existing.setUGender(gender);
                    existing.setUBirth(birth);
                    existing.setUProfile(profileImage);
                    existing.setUJoinType("naver");
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(
                        UserEntity.builder()
                                .uid(email != null ? email : nickname)
                                .uname(name)
                                .unickname(nickname)
                                .uGender(gender)
                                .uBirth(birth)
                                .uProfile(profileImage)
                                .uJoinType("naver")
                                .uStatus(9)   // 가입 미완료
                                .uWarn(0)
                                .uManner(20.0)
                                .uDate(LocalDateTime.now())
                                .build()
                ));

        // 4) 시큐리티 쪽으로 돌려줄 때는
        //    👉 "response" 말고 "id" 를 대표 키로 쓴다
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                response,          // ← 안쪽 map
                "id"               // ← 여기! response 안에 있는 실제 키
        );
    }

    private String buildBirth(String year, String birthday) {
        if (year != null && birthday != null) {
            return year + "-" + birthday;   // 1999-10-02
        }
        return birthday != null ? birthday : null;
    }
}
