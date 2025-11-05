package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationRepository;
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
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class OauthController {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;   // ✅ 주소 테이블 연동
    private final JwtProvider jwtProvider;

    /**
     * ✅ 현재 로그인한 사용자 정보 조회
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

        String sub = jwtProvider.getUid(accessToken);
        log.debug("JWT subject = {}", sub);

        UserEntity user = null;

        // 1️⃣ 이메일로 먼저 조회
        Optional<UserEntity> byUid = userRepository.findByUid(sub);
        if (byUid.isPresent()) {
            user = byUid.get();
        } else {
            // 2️⃣ 숫자(PK)일 경우
            try {
                Long id = Long.valueOf(sub);
                user = userRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignore) {}
        }

        if (user == null) {
            return ResponseEntity.ok(new MeResponse());
        }

        // ✅ location 테이블에서 주소 목록 가져오기
        List<LocationEntity> locations = locationRepository.findByUser(user);

        MeResponse resp = new MeResponse();
        resp.setU_id(user.getUid());
        resp.setU_name(user.getUname());
        resp.setU_nickname(user.getUnickname());
        resp.setU_phone(user.getUphone());
        resp.setU_gender(user.getUGender());
        resp.setU_birth(user.getUBirth());
        resp.setU_jointype(user.getUJoinType());
        resp.setU_status(user.getUStatus());

        // ✅ 주소 여러 개 리스트로 반환
        resp.setLocations(
                locations.stream().map(loc -> new MeResponse.LocationDTO(
                        loc.getLocKey(),
                        loc.getLocAddress(),
                        loc.getLocDetail(),
                        loc.isLocDefault(),
                        loc.getLocCode()

                )).toList()
        );

        return ResponseEntity.ok(resp);
    }

    /**
     * ✅ 소셜 로그인 이후 추가정보 저장
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

        // ✅ 기본 정보 업데이트
        if (StringUtils.hasText(dto.getEmail())) user.setUid(dto.getEmail());
        if (StringUtils.hasText(dto.getUname())) user.setUname(dto.getUname());
        if (StringUtils.hasText(dto.getNickname())) user.setUnickname(dto.getNickname());
        if (StringUtils.hasText(dto.getPhone())) user.setUphone(dto.getPhone());
        if (StringUtils.hasText(dto.getGender())) user.setUGender(dto.getGender());
        if (StringUtils.hasText(dto.getBirth())) user.setUBirth(dto.getBirth());
        if (StringUtils.hasText(dto.getProvider())) user.setUJoinType(dto.getProvider());
        if (user.getUDate() == null) user.setUDate(LocalDateTime.now());
        user.setUStatus(1);
        user.setUAgree("1".equals(dto.getAgree()) ? "1" : "0");

        userRepository.save(user);

        // ✅ 주소가 전달됐다면 location 테이블에 추가
        if (StringUtils.hasText(dto.getLocation()) ||
                StringUtils.hasText(dto.getAddressDetail()) ||
                StringUtils.hasText(dto.getAddress()) ||
                StringUtils.hasText(dto.locCode)) {

            LocationEntity loc = LocationEntity.builder()
                    .user(user)
                    .locAddress(dto.getLocation())
                    .locDetail(dto.getAddressDetail())
                    .locCode(dto.getLocCode())
                    .locDefault(true)
                    .build();
            locationRepository.save(loc);
        }

        return ResponseEntity.ok("OK");
    }

    /**
     * ✅ Authorization 헤더 or 쿠키에서 토큰 꺼내기
     */
    private String resolveToken(HttpServletRequest request, String authHeader) {
        // 1️⃣ 헤더에서 Bearer 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 2️⃣ 쿠키에서 꺼내기
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

    // ==========================================================
    // ✅ 내부 DTO 클래스들
    // ==========================================================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OAuthCompleteRequest {
        private String email;
        private String uname;
        private String nickname;
        private String phone;
        private String gender;
        private String birth;

        private String address;        // 주소
        private String addressDetail;  // 상세주소
        private String location;       // 거래 주소
        private String locCode;        // 우편번호

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
        private String u_jointype;
        private Integer u_status;

        // ✅ 주소 여러 개
        private List<LocationDTO> locations;

        // 주소 한 건 표현용
        public record LocationDTO(
                Long locKey,
                String locAddress,
                String locDetail,
                boolean locDefault,
                String locCode
        ) {}
    }
}
