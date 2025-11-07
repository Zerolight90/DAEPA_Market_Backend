package com.daepamarket.daepa_market_backend.user;

import com.daepamarket.daepa_market_backend.admin.user.UserResponseDTO;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutEntity;
import com.daepamarket.daepa_market_backend.domain.getout.GetoutRepository;
import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserLoginDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserSignUpDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.CookieProps;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProps;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

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

    public boolean existsByuId(String uId) {
        return userRepository.existsByUid(uId);
    }

    public boolean existsByuNickname(String uNickname) {
        return userRepository.existsByUnickname(uNickname);
    }

    public boolean existsByuPhone(String uPhone) {
        return userRepository.existsByUphone(uPhone);
    }

    //로그인 후 사용자 정보를 가지고 오는 함수
    public UserEntity findUserByuId(String uid) {
        return userRepository.findByUid(uid)
                .orElseThrow(() -> new RuntimeException("찾을 수 없는 유저: " + uid));
    }

    @Transactional
    public Long signup(UserSignUpDTO rep) {
        //중복 검사
        if(userRepository.existsByUid(rep.getU_id())){
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
        if(userRepository.existsByUnickname(rep.getU_nickname())){
            throw new IllegalStateException("이미 존재하는 별명입니다.");
        }
        if(userRepository.existsByUphone(rep.getU_phone())){
            throw new IllegalStateException("이미 존재하는 전화번호입니다.");
        }

        //비밀번호 암호화
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

        // 2) 주소가 들어왔다면 location 테이블에 따로 저장
        if (rep.getU_address() != null || rep.getU_location() != null || rep.getU_location_detail() != null) {
            LocationEntity loc = LocationEntity.builder()
                    .user(user)
                    .locAddress(rep.getU_location())         // → 너희가 의미 맞게 맵핑해
                    .locDetail(rep.getU_location_detail())
                    .locCode(rep.getU_locCode())
                    .locDefault(true)
                    .build();
            locationRepository.save(loc);
        }

        return user.getUIdx();
    }

    //로그인
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
        //DB에 저장
        userRepository.save(user);

        ResponseCookie a = cookieUtil.accessCookie(access, Duration.ofMinutes(jwtProps.getAccessExpMin()));
        ResponseCookie r = cookieUtil.refreshCookie(refresh, Duration.ofDays(jwtProps.getRefreshExpDays()));

        //프론트로 보낼 데이터
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
        // 리프레시 토큰 쿠키 확인
        String refresh = readCookie(request, CookieUtil.REFRESH)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "리프레시 쿠키 없음"));

        // 토큰 만료 여부 검증
        if (jwtProvider.isExpired(refresh)) {
            throw new ResponseStatusException(UNAUTHORIZED, "리프레시 토큰 만료");
        }

        // DB에서 사용자 찾기 (저장된 refreshToken으로)
        UserEntity user = userRepository.findByUrefreshToken(refresh)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "유효하지 않은 리프레시 토큰"));

        // 새 토큰 발급
        String newAccess = jwtProvider.createAccessToken(String.valueOf(user.getUIdx()), user.getUType());
        String newRefresh = jwtProvider.createRefreshToken(String.valueOf(user.getUIdx()));

        // 새 리프레시 토큰 저장 (회전)
        user.setUrefreshToken(newRefresh);
        userRepository.save(user);

        // 쿠키 재설정
        ResponseCookie accessCookie = cookieUtil.accessCookie(
                newAccess, Duration.ofMinutes(jwtProps.getAccessExpMin()));
        ResponseCookie refreshCookie = cookieUtil.refreshCookie(
                newRefresh, Duration.ofDays(jwtProps.getRefreshExpDays()));

        // 응답 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("message", "토큰 재발급 완료"));
    }

    @Transactional
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // 쿠키에서 refresh 토큰 꺼내기
        String refresh = readCookie(request, CookieUtil.REFRESH)
                .orElse(null);

        // DB에 refresh가 등록된 사용자 찾고 무효화
        if (refresh != null) {
            userRepository.findByUrefreshToken(refresh).ifPresent(user -> {
                user.setUrefreshToken(null);  // 서버에 저장된 리프레시 제거
                userRepository.save(user);
            });
        }

        // 브라우저 쿠키 삭제 (access + refresh)
        ResponseCookie clearAccess = cookieUtil.clear(CookieUtil.ACCESS);
        ResponseCookie clearRefresh = cookieUtil.clear(CookieUtil.REFRESH);

        // 응답 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .body(Map.of("message", "로그아웃 완료"));
    }


    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    //로그인한 회원 정보 반환
    public ResponseEntity<?> getMe(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("토큰이 없습니다.");
            }

            String token = auth.substring(7);
            // 토큰 검증
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            // 토큰에서 사용자 ID 꺼내기
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            // DB에서 유저 조회
            var user = userRepository.findById(userId)
                    .orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
            }
            List<LocationEntity> locations = locationRepository.findByUser(user);
            // 필요한 정보만 리턴
            Map<String, Object> result = new HashMap<>();
            result.put("uIdx", user.getUIdx());
            result.put("uName", user.getUname());
            result.put("uId", user.getUid());
            result.put("uManner", user.getUManner());
            result.put("uPhone", user.getUphone());
            result.put("uNickname", user.getUnickname());
            result.put("u_nickname", user.getUnickname());
            // 주소는 이제 별도 리스트로 내려줌
            result.put("locations", locations.stream().map(loc -> Map.of(
                    "locKey", loc.getLocKey(),
                    "locAddress", loc.getLocAddress(),
                    "locCode", loc.getLocCode(),
                    "locDetail", loc.getLocDetail(),
                    "locDefault", loc.isLocDefault(),
                    "locTitle", loc.getLocTitle(),
                    "locName", loc.getLocName(),
                    "locNum", loc.getLocNum()

            )).toList());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    public UserEntity findUserById (Long user){
        return userRepository.findById(user)
                .orElseThrow(() -> new RuntimeException("User Not Found: " + user));
    }

    /* 관리자용 전체 사용자 조회 */
    public List<UserResponseDTO> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDTO::of)
                .toList();
    }

    //이름이랑 전화번호를 통해 아이디 찾기
    public Optional<UserEntity> findByUNameAndUphone(String uname, String uphone){
        String phoneNumber = uphone.replaceAll("[^0-9]", "");
        return userRepository.findByUnameAndUphone(uname, phoneNumber);
    }

    //아이디, 이름, 전화번호를 통해 비밀번호 재설정 전 본인확인
    public Optional<UserEntity> findByUidAndUnameAndUphone(String uid, String uname, String uphone){
        String phoneNumber = uphone.replaceAll("[^0-9]", "");
        return userRepository.findByUidAndUnameAndUphone(uid, uname, phoneNumber);
    }


    //비밀번호 재설정
    public void reset_password(String uId, String newPw) {
        //아이디로 사용자 조회
        UserEntity user = userRepository.findByUid(uId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String encodedPassword = passwordEncoder.encode(newPw);

        user.setUPw(encodedPassword);

        userRepository.save(user);
    }

    //탈퇴
    @Transactional
    public void bye(HttpServletRequest request, Map<String, Object> body) {
        // 1) 토큰에서 유저 아이디 꺼내기
        String auth = request.getHeader("Authorization");

        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(UNAUTHORIZED, "토큰이 없습니다.");
        }
        String token = auth.substring(7);

        if (jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(UNAUTHORIZED, "토큰이 만료되었습니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));

        // 실제 유저 가져오기
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 프론트에서 온 데이터 파싱
        @SuppressWarnings("unchecked")
        var reasons = (java.util.List<String>) body.getOrDefault("reasons", java.util.List.of());
        String etc = (String) body.getOrDefault("etc", "");

        // 여러 개 선택한 거 전부 저장하는 버전
        String goStatus = mapReasonsToStatusMulti(reasons, etc);

        // getout 테이블에 기록 저장
        GetoutEntity log = GetoutEntity.builder()
                .user(user)
                .goStatus(goStatus)
                .goOutdata(LocalDateTime.now().toLocalDate())
                .build();
        getoutRepository.save(log);

        // 6) 사용자 상태 변경
        user.setUStatus(2);  // 탈퇴
        userRepository.save(user);
    }

    /**
     * 여러 개 체크했을 때 전부 저장하는 버전
     * 예) ["low_usage","ux_issues"]  → "1,3"
     * 예) ["low_usage","etc"] + etc="기타 사유" → "1,기타 사유"
     */
    private String mapReasonsToStatusMulti(java.util.List<String> reasons, String etc) {
        if (reasons == null || reasons.isEmpty()) {
            return "0";
        }

        java.util.List<String> mapped = new java.util.ArrayList<>();

        for (String r : reasons) {
            switch (r) {
                case "low_usage" -> mapped.add("1");
                case "bad_users" -> mapped.add("2");
                case "ux_issues" -> mapped.add("3");
                case "temporary" -> mapped.add("4");
                case "etc" -> {
                    // 기타 선택했고 내용이 있으면 그걸 그대로 넣기
                    if (etc != null && !etc.isBlank()) {
                        mapped.add(etc.trim());
                    } else {
                        mapped.add("기타");
                    }
                }
                default -> mapped.add("0");
            }
        }

        // "1,2,3" 이런 식으로 저장
        return String.join(",", mapped);
    }








}
