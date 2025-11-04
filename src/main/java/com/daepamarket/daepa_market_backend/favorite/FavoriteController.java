package com.daepamarket.daepa_market_backend.favorite;

import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final JwtProvider jwtProvider;

    // 찜 토글 (로그인 필수)
    @PostMapping("/{productId}/toggle")
    public ResponseEntity<?> toggle(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = requireUserId(request);
        boolean now = favoriteService.toggle(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(now, count));
    }

    // 상품 카드가 호출하는 곳 (비로그인도 들어옴)
    @GetMapping("/{productId}")
    public ResponseEntity<?> getStatus(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = optionalUserId(request);   // 여기서 예외 안나게 하는 게 핵심
        boolean favorited = false;
        if (userId != null) {
            favorited = favoriteService.isFavorited(userId, productId);
        }
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(favorited, count));
    }

    // 내 찜 목록 (로그인 필수)
    @GetMapping("")
    public ResponseEntity<List<FavoriteItemDTO>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ResponseEntity.ok(favoriteService.list(userId));
    }

    // ----------------- 내부 유틸 -----------------

    // 반드시 로그인해야 하는 경우
    private Long requireUserId(HttpServletRequest request) {
        String token = extractAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        try {
            if (jwtProvider.isExpired(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            }
            return Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            // 토큰이 깨졌을 때도 401
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }

    // 로그인 안 했으면 null 주는 버전 (여기가 지금 문제였음)
    private Long optionalUserId(HttpServletRequest request) {
        String token = extractAccessToken(request);
        if (token == null) {
            return null;
        }
        try {
            if (jwtProvider.isExpired(token)) {
                return null;
            }
            return Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            // 토큰 파싱 실패, 시그니처 에러 등 → 그냥 비로그인으로 처리
            return null;
        }
    }

    // 쿠키나 Authorization 헤더에서 accessToken 뽑기
    private String extractAccessToken(HttpServletRequest request) {
        // 1) 쿠키 우선
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("accessToken".equalsIgnoreCase(c.getName()) || "ACCESS_TOKEN".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        // 2) Authorization: Bearer ...
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    // 응답용 record
    private record FavoriteResponse(boolean favorited, long count) {}
}
