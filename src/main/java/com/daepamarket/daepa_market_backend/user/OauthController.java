package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class OauthController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    /**
     * 소셜/로컬 공통: 현재 로그인한 사용자 정보
     * - 토큰의 subject 가 "이메일"일 수도 있고
     * - "u_idx" 같은 숫자일 수도 있으니까 둘 다 처리한다.
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        String accessToken = resolveToken(request, authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("토큰이 없습니다.");
        }

        // 토큰에 들어있는 subject (이게 이메일일 수도, 숫자일 수도 있음)
        String sub = jwtProvider.getUid(accessToken);
        log.debug("JWT subject = {}", sub);

        UserEntity user = null;

        // 1) 이메일로 먼저 찾아본다
        Optional<UserEntity> byUid = userRepository.findByUid(sub);
        if (byUid.isPresent()) {
            user = byUid.get();
        } else {
            // 2) 이메일이 아니면 숫자일 가능성 → PK로 한 번 더 찾기
            try {
                Long id = Long.valueOf(sub);
                user = userRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignore) {
                // 진짜 이메일도 아니고 숫자도 아니면 못 찾는 거
            }
        }

        if (user == null) {
            // 유저 없으면 비어있는 응답
            return ResponseEntity.ok(new MeResponse());
        }

        MeResponse resp = new MeResponse();
        resp.setU_id(user.getUid());                 // 이메일/아이디
        resp.setU_name(user.getUname());             // 이름
        resp.setU_nickname(user.getUnickname());     // ✅ 닉네임
        resp.setU_phone(user.getUphone());
        resp.setU_gender(user.getUGender());
        resp.setU_birth(user.getUBirth());
        resp.setU_address(user.getUAddress());               // 우편번호
        resp.setU_location(user.getULocation());             // 주소
        resp.setU_location_detail(user.getULocationDetail()); // 상세주소
        resp.setU_jointype(user.getUJoinType());
        resp.setU_status(user.getUStatus());

        return ResponseEntity.ok(resp);
    }

    /**
     * 소셜 로그인 이후 추가정보 저장
     */
    @PostMapping("/oauth-complete")
    public ResponseEntity<?> oauthComplete(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody OAuthCompleteRequest dto
    ) {

        String accessToken = resolveToken(request, authHeader);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("토큰이 없습니다.");
        }

        String sub = jwtProvider.getUid(accessToken);

        // 위랑 똑같이 이메일/숫자 둘 다 처리
        UserEntity user = userRepository.findByUid(sub).orElseGet(() -> {
            try {
                Long id = Long.valueOf(sub);
                return userRepository.findById(id).orElse(null);
            } catch (NumberFormatException e) {
                return null;
            }
        });

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 사용자를 찾을 수 없습니다: " + sub);
        }

        // ===== 기본 정보 =====
        if (StringUtils.hasText(dto.getEmail())) {
            user.setUid(dto.getEmail());
        }
        if (StringUtils.hasText(dto.getUname())) {
            user.setUname(dto.getUname());
        }
        if (StringUtils.hasText(dto.getNickname())) {
            user.setUnickname(dto.getNickname());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            user.setUphone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getGender())) {
            user.setUGender(dto.getGender());
        }
        if (StringUtils.hasText(dto.getBirth())) {
            user.setUBirth(dto.getBirth());
        }

        // ===== 주소 =====
        if (StringUtils.hasText(dto.getLocation())) {
            user.setULocation(dto.getLocation());
        }
        if (StringUtils.hasText(dto.getAddressDetail())) {
            user.setULocationDetail(dto.getAddressDetail());
        }
        if (StringUtils.hasText(dto.getAddress())) {
            user.setUAddress(dto.getAddress());
        }

        // 소셜 종류
        if (StringUtils.hasText(dto.getProvider())) {
            user.setUJoinType(dto.getProvider());
        }

        // 가입일
        if (user.getUDate() == null) {
            user.setUDate(LocalDateTime.now());
        }

        // 선택동의
        user.setUAgree("1".equals(dto.getAgree()) ? "1" : "0");

        // 정상회원
        user.setUStatus(1);

        userRepository.save(user);

        return ResponseEntity.ok("OK");
    }

    /**
     * Authorization 헤더 or 쿠키에서 토큰 꺼내기
     */
    private String resolveToken(HttpServletRequest request, String authHeader) {
        // 1) 헤더
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2) 쿠키
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(c -> {
                        String name = c.getName();
                        return "accessToken".equalsIgnoreCase(name)
                                || "ACCESS_TOKEN".equalsIgnoreCase(name)
                                || "jwt".equalsIgnoreCase(name);
                    })
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }

        return null;
    }

    // ================= DTO =================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAuthCompleteRequest {
        private String email;
        private String uname;
        private String nickname;
        private String phone;
        private String gender;
        private String birth;

        private String address;        // 우편번호
        private String addressDetail;  // 상세주소
        private String location;       // 주소

        private String provider;       // naver/kakao
        private String agree;          // "1" or "0"
    }

    @Data
    public static class MeResponse {
        private String u_id;
        private String u_name;
        private String u_nickname;
        private String u_phone;
        private String u_gender;
        private String u_birth;
        private String u_address;
        private String u_location;
        private String u_location_detail;
        private String u_jointype;
        private Integer u_status;
    }
}
