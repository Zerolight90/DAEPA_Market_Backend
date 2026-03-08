package com.daepamarket.daepa_market_backend.favorite;

import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil; // ✅ 의존성 주입 완료

    @PostMapping("/{productId}/toggle")
    public ResponseEntity<?> toggle(HttpServletRequest request, @PathVariable("productId") Long productId) {
        Long userId = requireUserId(request);
        boolean now = favoriteService.toggle(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(now, count));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> getStatus(HttpServletRequest request, @PathVariable("productId") Long productId) {
        Long userId = optionalUserId(request);
        boolean favorited = (userId != null) && favoriteService.isFavorited(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(favorited, count));
    }

    @GetMapping("")
    public ResponseEntity<List<FavoriteItemDTO>> list(HttpServletRequest request) {
        Long userId = optionalUserId(request);
        if (userId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(favoriteService.list(userId));
    }

    // ================== 내부 유틸 ==================
    private Long requireUserId(HttpServletRequest request) {
        Long userId = optionalUserId(request);
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        return userId;
    }

    private Long optionalUserId(HttpServletRequest request) {
        String token = cookieUtil.getAccessTokenFromCookie(request); // ✅ 깔끔하게 토큰 추출
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            return null;
        }
        return Long.valueOf(jwtProvider.getUid(token));
    }

    private record FavoriteResponse(boolean favorited, long count) {}
}