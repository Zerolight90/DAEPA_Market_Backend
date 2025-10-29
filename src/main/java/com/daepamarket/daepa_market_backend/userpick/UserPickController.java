package com.daepamarket.daepa_market_backend.userpick;

import java.util.List;
import java.util.Map;

import com.daepamarket.daepa_market_backend.domain.userpick.UserPickEntity;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/userpicks")
@CrossOrigin(origins = "http://localhost:3000")
public class UserPickController {

    private final UserPickService userPickService;
    private final UserService userService; // 예시: 현재 로그인한 유저 정보를 가져오기 위한 서비스

    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    // GET /api/userpicks : 현재 로그인한 유저의 관심 상품 목록 조회
    @GetMapping
    public ResponseEntity<?> getUserPicks(HttpServletRequest request) {
        // 중요: 실제로는 Spring Security 등 보안 설정을 통해
        // 현재 로그인한 사용자의 정보를 가져와야 합니다.
        // 여기서는 임시로 ID가 김토스인 사용자를 조회한다고 가정합니다.
        String token = resolveAccessToken(request); // 토큰 추출
        if (token == null) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다 (토큰 없음)."));
        }
        if (jwtProvider.isExpired(token)) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "토큰이 만료되었습니다."));
        }

        // ===== 2. 토큰에서 사용자 ID 추출 =====
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token)); // 토큰 subject가 uIdx (문자열)라고 가정
            System.out.println("### 추출된 User ID: " + userId); // ✅ 성공 시 로그 추가!
        } catch (Exception e) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }
        // =======================================================

        UserEntity currentUser = userService.findUserById(userId);

        List<UserPickCreateRequestDto> picks = userPickService.findPicksByUser(currentUser);
        return ResponseEntity.ok(picks);
    }

    // DELETE /api/userpicks/{id} : 특정 관심 상품 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUserPick(@PathVariable("id") Long upIdx) {
        // TODO: 삭제를 요청한 사용자가 해당 관심 상품의 실제 소유주인지 확인하는 로직 추가 필요
        userPickService.deletePick(upIdx);
        return ResponseEntity.ok("성공적으로 삭제되었습니다.");
    }

    @PostMapping("/add")
    public ResponseEntity<?> createUserPick(@RequestBody UserPickCreateRequestDto requestDto, HttpServletRequest request) {
        // 실제로는 Spring Security 등에서 현재 로그인한 유저 정보를 가져와야 함
        String token = resolveAccessToken(request); // 토큰 추출
        if (token == null) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다 (토큰 없음)."));
        }
        if (jwtProvider.isExpired(token)) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "토큰이 만료되었습니다."));
        }

        // ===== 2. 토큰에서 사용자 ID 추출 =====
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token)); // 토큰 subject가 uIdx (문자열)라고 가정
            System.out.println("### 추출된 User ID: " + userId); // ✅ 성공 시 로그 추가!
        } catch (Exception e) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }
        // =======================================================

        UserEntity currentUser = userService.findUserById(userId);

        UserPickCreateRequestDto createdPick = userPickService.createPick(requestDto, currentUser);

        // 성공 시 201 Created 상태와 함께 생성된 데이터를 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPick);
    }



    // --- 토큰 추출 헬퍼 메소드 ---
    private String resolveAccessToken(HttpServletRequest request) {
        // 1) 쿠키 우선
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                // ✅ CookieUtil.ACCESS (쿠키 이름) 확인!
                if (CookieUtil.ACCESS.equals(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) return v;
                }
            }
        }
        // 2) Authorization: Bearer 헤더
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null; // 토큰 못 찾음
    }

}
