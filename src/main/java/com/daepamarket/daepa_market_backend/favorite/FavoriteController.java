package com.daepamarket.daepa_market_backend.favorite;

import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final JwtProvider jwtProvider;

    @PostMapping("/{productId}/toggle")
    public ResponseEntity<?> toggle(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = requireUserId(request);
        boolean now = favoriteService.toggle(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(now, count));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getStatus(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = optionalUserId(request);
        boolean fav = (userId != null) && favoriteService.isFavorited(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(fav, count));
    }

    private Long requireUserId(HttpServletRequest request) {
        String token = extractAccessToken(request);
        if (token == null || jwtProvider.isExpired(token)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."
            );
        }
        return Long.valueOf(jwtProvider.getUid(token));
    }

    private Long optionalUserId(HttpServletRequest request) {
        String token = extractAccessToken(request);
        if (token == null || jwtProvider.isExpired(token)) return null;
        try { return Long.valueOf(jwtProvider.getUid(token)); } catch (Exception ignore) { return null; }
    }

    private String extractAccessToken(HttpServletRequest request) {
        // 1) 쿠키
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                // ✅ 대소문자 무시 + 대문자 이름도 함께 허용
                if ("accessToken".equalsIgnoreCase(c.getName()) || "ACCESS_TOKEN".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        // 2) 헤더
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7);
        return null;
    }
    @GetMapping("") // /api/favorites
    public ResponseEntity<List<FavoriteItemDTO>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        return ResponseEntity.ok(favoriteService.list(userId));
    }


    private record FavoriteResponse(boolean favorited, long count) {}
}

