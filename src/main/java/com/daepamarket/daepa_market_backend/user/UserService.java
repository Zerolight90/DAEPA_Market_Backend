package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.S3Service;
import com.daepamarket.daepa_market_backend.admin.user.UserResponseDTO;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutEntity;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutRepository;
import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationRepository;
import com.daepamarket.daepa_market_backend.domain.user.*;
import com.daepamarket.daepa_market_backend.jwt.CookieProps;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProps;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProps jwtProps;
    private final CookieUtil cookieUtil;
    private final CookieProps cookieProps;
    private final JwtProvider jwtProvider;
    private final GetoutRepository getoutRepository;
    private final S3Service s3Service;

    public boolean existsByuId(String uId) { return userRepository.existsByUid(uId); }
    public boolean existsByuNickname(String uNickname) { return userRepository.existsByUnickname(uNickname); }
    public boolean existsByuPhone(String uPhone) { return userRepository.existsByUphone(uPhone); }

    public UserEntity findUserByuId(String uid) {
        return userRepository.findByUid(uid).orElseThrow(() -> new RuntimeException("찾을 수 없는 유저: " + uid));
    }

    @Transactional
    public Long signup(UserSignUpDTO rep) {
        if(userRepository.existsByUid(rep.getU_id())) throw new IllegalStateException("이미 존재하는 이메일입니다.");
        if(userRepository.existsByUnickname(rep.getU_nickname())) throw new IllegalStateException("이미 존재하는 별명입니다.");
        if(userRepository.existsByUphone(rep.getU_phone())) throw new IllegalStateException("이미 존재하는 전화번호입니다.");

        String encodedPassword = passwordEncoder.encode(rep.getU_pw());
        LocalDateTime now = LocalDateTime.now();

        UserEntity user = userRepository.save(UserEntity.builder()
                .uid(rep.getU_id())
                .uPw(encodedPassword)
                .uname(rep.getU_name())
                .unickname(rep.getU_nickname())
                .uphone(rep.getU_phone())
                .uBirth(rep.getU_birth())
                .uGender(rep.getU_gender())
                .uDate(now)
                .uAgree(rep.getU_agree())
                .uJoinType("로컬")
                .uStatus(1)
                .uWarn(0)
                .uManner(20.0)
                .build()
        );

        if (rep.getU_address() != null || rep.getU_location() != null || rep.getU_location_detail() != null) {
            LocationEntity loc = LocationEntity.builder()
                    .user(user)
                    .locAddress(rep.getU_location())
                    .locDetail(rep.getU_location_detail())
                    .locCode(rep.getU_locCode())
                    .locDefault(true)
                    .build();
            locationRepository.save(loc);
        }
        return user.getUIdx();
    }

    @Transactional
    public ResponseEntity<?> login(UserLoginDTO dto) {
        UserEntity user = userRepository.findByUid(dto.getU_id())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "존재하지 않는 계정"));

        if (!passwordEncoder.matches(dto.getU_pw(), user.getUPw())) {
            throw new ResponseStatusException(UNAUTHORIZED, "비밀번호 불일치");
        }
        if (user.getUStatus() == 2) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다.");
        }

        String role = user.getUType() != null ? user.getUType() : "USER";
        String access = jwtProvider.createAccessToken(String.valueOf(user.getUIdx()), role);
        String refresh = jwtProvider.createRefreshToken(String.valueOf(user.getUIdx()));

        user.setUrefreshToken(refresh);
        userRepository.save(user);

        ResponseCookie a = cookieUtil.accessCookie(access, Duration.ofMinutes(jwtProps.getAccessExpMin()));
        ResponseCookie r = cookieUtil.refreshCookie(refresh, Duration.ofDays(jwtProps.getRefreshExpDays()));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("u_idx", user.getUIdx());
        responseBody.put("u_id", user.getUid());
        responseBody.put("u_name", user.getUname());
        responseBody.put("u_type", role);
        responseBody.put("accessToken", access);
        responseBody.put("message", "로그인 성공");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, a.toString())
                .header(HttpHeaders.SET_COOKIE, r.toString())
                .body(responseBody);
    }

    @Transactional
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        String refresh = readCookie(request, CookieUtil.REFRESH)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "리프레시 쿠키 없음"));

        if (jwtProvider.isExpired(refresh)) {
            throw new ResponseStatusException(UNAUTHORIZED, "리프레시 토큰 만료");
        }

        UserEntity user = userRepository.findByUrefreshToken(refresh)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "유효하지 않은 리프레시 토큰"));

        String newAccess = jwtProvider.createAccessToken(String.valueOf(user.getUIdx()), user.getUType());
        String newRefresh = jwtProvider.createRefreshToken(String.valueOf(user.getUIdx()));

        user.setUrefreshToken(newRefresh);
        userRepository.save(user);

        ResponseCookie accessCookie = cookieUtil.accessCookie(newAccess, Duration.ofMinutes(jwtProps.getAccessExpMin()));
        ResponseCookie refreshCookie = cookieUtil.refreshCookie(newRefresh, Duration.ofDays(jwtProps.getRefreshExpDays()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "토큰 재발급 완료"));
    }

    @Transactional
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String refresh = readCookie(request, CookieUtil.REFRESH).orElse(null);
        if (refresh != null) {
            userRepository.findByUrefreshToken(refresh).ifPresent(user -> {
                user.setUrefreshToken(null);
                userRepository.save(user);
            });
        }
        ResponseCookie clearAccess = cookieUtil.clear(CookieUtil.ACCESS);
        ResponseCookie clearRefresh = cookieUtil.clear(CookieUtil.REFRESH);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .body(Map.of("message", "로그아웃 완료"));
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return Optional.ofNullable(cookie.getValue());
        }
        return Optional.empty();
    }

    // ✅ 로그인한 회원 정보 반환 (500 에러 폭탄 제거 완료!)
    public ResponseEntity<?> getMe(HttpServletRequest request) {
        try {
            String token = cookieUtil.getAccessTokenFromCookie(request);
            if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            UserEntity user = userRepository.findById(uIdx).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
            }

            List<LocationEntity> locations = locationRepository.findByUser(user);

            Map<String, Object> result = new HashMap<>();
            result.put("uIdx", user.getUIdx());
            result.put("uName", user.getUname());
            result.put("uId", user.getUid());
            result.put("uManner", user.getUManner());
            result.put("uPhone", user.getUphone());
            result.put("uNickname", user.getUnickname());
            result.put("u_nickname", user.getUnickname());
            result.put("u_profile", user.getUProfile());
            
            // 🚨 Map.of() 대신 HashMap 사용하여 null 허용!
            result.put("locations", locations.stream().map(loc -> {
                Map<String, Object> locMap = new HashMap<>();
                locMap.put("locKey", loc.getLocKey());
                locMap.put("locAddress", loc.getLocAddress());
                locMap.put("locCode", loc.getLocCode());
                locMap.put("locDetail", loc.getLocDetail()); // null이어도 에러 안 남
                locMap.put("locDefault", loc.isLocDefault());
                return locMap;
            }).toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("getMe 에러 발생: ", e);
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    public UserEntity findUserById (Long user){
        return userRepository.findById(user).orElseThrow(() -> new RuntimeException("User Not Found: " + user));
    }

    public Optional<UserEntity> findByUNameAndUphone(String uname, String uphone){
        String phoneNumber = uphone.replaceAll("[^0-9]", "");
        return userRepository.findByUnameAndUphone(uname, phoneNumber);
    }

    public Optional<UserEntity> findByUidAndUnameAndUphone(String uid, String uname, String uphone){
        String phoneNumber = uphone.replaceAll("[^0-9]", "");
        return userRepository.findByUidAndUnameAndUphone(uid, uname, phoneNumber);
    }

    public void reset_password(String uId, String newPw) {
        UserEntity user = userRepository.findByUid(uId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        String encodedPassword = passwordEncoder.encode(newPw);
        user.setUPw(encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    public void bye(HttpServletRequest request, Map<String, Object> body) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        @SuppressWarnings("unchecked")
        var reasons = (java.util.List<String>) body.getOrDefault("reasons", java.util.List.of());
        String etc = (String) body.getOrDefault("etc", "");

        String goStatus = mapReasonsToStatusMulti(reasons, etc);

        GetoutEntity log = GetoutEntity.builder()
                .user(user)
                .goStatus(goStatus)
                .goOutdata(LocalDateTime.now().toLocalDate())
                .build();
        getoutRepository.save(log);

        user.setUStatus(2);  // 탈퇴
        userRepository.save(user);
    }

    private String mapReasonsToStatusMulti(java.util.List<String> reasons, String etc) {
        if (reasons == null || reasons.isEmpty()) return "0";

        java.util.List<String> mapped = new java.util.ArrayList<>();
        for (String r : reasons) {
            switch (r) {
                case "low_usage" -> mapped.add("1");
                case "bad_users" -> mapped.add("2");
                case "ux_issues" -> mapped.add("3");
                case "temporary" -> mapped.add("4");
                case "etc" -> mapped.add((etc != null && !etc.isBlank()) ? etc.trim() : "기타");
                default -> mapped.add("0");
            }
        }
        return String.join(",", mapped);
    }

    @Transactional
    public String uploadProfile(HttpServletRequest request, MultipartFile file) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String url;
        try {
            url = s3Service.uploadFile(file, "profiles");
        } catch (IOException e) {
            log.error("프로필 업로드 중 오류", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "프로필 업로드 실패");
        }

        user.setUProfile(url);
        userRepository.save(user);

        return url;
    }

    @Transactional
    public void updateMe(HttpServletRequest request, UserUpdateDTO dto) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (dto.getNickname() != null && !dto.getNickname().isBlank()) {
            if (!dto.getNickname().equals(user.getUnickname())) {
                if (userRepository.existsByUnickname(dto.getNickname())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 별명입니다.");
                }
                user.setUnickname(dto.getNickname());
            }
        }

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            String enc = passwordEncoder.encode(dto.getNewPassword());
            user.setUPw(enc);
        }

        if (dto.getGender() != null && !dto.getGender().isBlank()) {
            user.setUGender(dto.getGender());
        }

        if (dto.getBirth() != null && !dto.getBirth().isBlank()) {
            user.setUBirth(dto.getBirth());
        }

        if (dto.getProfile() != null && !dto.getProfile().isBlank()) {
            user.setUProfile(dto.getProfile());
        }
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAllUsers() {
        List<UserEntity> users = userRepository.findAll();
        return users.stream()
                .map(user -> {
                    List<LocationEntity> locations = locationRepository.findByUserId(user.getUIdx());
                    String location = null;
                    if (locations != null && !locations.isEmpty()) {
                        LocationEntity defaultLoc = locations.stream()
                                .filter(LocationEntity::isLocDefault)
                                .findFirst()
                                .orElse(locations.get(0));
                        if (defaultLoc != null) {
                            String address = defaultLoc.getLocAddress() != null ? defaultLoc.getLocAddress().trim() : "";
                            String detail = defaultLoc.getLocDetail() != null ? defaultLoc.getLocDetail().trim() : "";
                            location = address + (detail.isEmpty() ? "" : " " + detail);
                        }
                    }
                    return UserResponseDTO.of(user, location);
                })
                .toList();
    }
}