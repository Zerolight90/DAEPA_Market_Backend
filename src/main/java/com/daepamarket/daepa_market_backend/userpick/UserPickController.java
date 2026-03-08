package com.daepamarket.daepa_market_backend.userpick;

import java.util.List;
import java.util.Map;

import com.daepamarket.daepa_market_backend.domain.userpick.UserPickEntity;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.user.UserService;
import com.daepamarket.daepa_market_backend.product.ProductNotificationDTO;
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
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    // ✅ 만능 열쇠 헬퍼 메서드
    private String extractToken(HttpServletRequest request) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            return null;
        }
        return token;
    }

    @GetMapping
    public ResponseEntity<?> getUserPicks(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요하거나 토큰이 만료되었습니다."));
        }

        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }

        UserEntity currentUser = userService.findUserById(userId);
        List<UserPickCreateRequestDto> picks = userPickService.findPicksByUser(currentUser);
        return ResponseEntity.ok(picks);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUserPick(@PathVariable("id") Long upIdx) {
        userPickService.deletePick(upIdx);
        return ResponseEntity.ok("성공적으로 삭제되었습니다.");
    }

    @PostMapping("/add")
    public ResponseEntity<?> createUserPick(@RequestBody UserPickCreateRequestDto requestDto, HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요하거나 토큰이 만료되었습니다."));
        }

        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }

        UserEntity currentUser = userService.findUserById(userId);
        UserPickCreateRequestDto createdPick = userPickService.createPick(requestDto, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdPick);
    }

    @PostMapping("/notifications")
    public ResponseEntity<?> getNotifications(@RequestBody UserPickDTO pick, HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증이 필요합니다."));
        }
        Long userId = Long.valueOf(jwtProvider.getUid(token));

        List<ProductNotificationDTO> notifications = userPickService.getNotificationsForPick(pick, userId);
        return ResponseEntity.ok(notifications);
    }
}