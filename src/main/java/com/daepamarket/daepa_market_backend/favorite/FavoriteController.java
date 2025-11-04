package com.daepamarket.daepa_market_backend.favorite;

import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
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

    /**
     * 찜 토글 (로그인 필수)
     */
    @PostMapping("/{productId}/toggle")
    public ResponseEntity<?> toggle(HttpServletRequest request,
                                    @PathVariable Long productId) {
        Long userId = requireUserId(request); // 여긴 진짜 로그인 필요
        boolean now = favoriteService.toggle(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(now, count));
    }

    /**
     * 상품 카드에서 찜 상태/개수 조회 (비로그인도 허용)
     */
    @GetMapping("/{productId}")
    public ResponseEntity<?> getStatus(HttpServletRequest request,
                                       @PathVariable Long productId) {
        Long userId = optionalUserId(request);  // 없으면 null
        boolean favorited = false;
        if (userId != null) {
            favorited = favoriteService.isFavorited(userId, productId);
        }
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(favorited, count));
    }

    /**
     * 내 찜 목록
     * 👉 이게 문제였으니까: 로그인 안 돼 있으면 401 말고 [] 반환
     */
    @GetMapping("")
    public ResponseEntity<List<FavoriteItemDTO>> list(HttpServletRequest request) {
        Long userId = optionalUserId(request);
        if (userId == null) {
            // 새로고침했는데 토큰 안 왔을 때 여기로 온다
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(favoriteService.list(userId));
    }

    // ================== 내부 유틸 ==================

    /** 로그인 필수 버전 */
    private Long requireUserId(HttpServletRequest request) {
        Long userId = optionalUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }

    /**
     * 로그인 안 해도 되는 곳에서 쓰는 버전
     * - Authorization: Bearer ... 먼저 보고
     * - 없으면 쿠키에서 ACCESS_TOKEN / accessToken 찾고
     * - 토큰이 이상하면 null
     */
    private Long optionalUserId(HttpServletRequest request) {
        String token = null;

        // 1) 헤더 먼저
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }

        // 2) 쿠키에서도 찾아봄 (프론트가 쿠키로만 로그인했을 수도 있으니까)
        if (token == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("ACCESS_TOKEN".equals(c.getName())
                            || "accessToken".equalsIgnoreCase(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }

        if (token == null) {
            return null;
        }

        // 3) JWT 파싱
        try {
            // 만료면 null
            if (jwtProvider.isExpired(token)) {
                return null;
            }
            String uid = jwtProvider.getUid(token);
            if (uid == null) return null;
            return Long.valueOf(uid);
        } catch (Exception e) {
            // 여기서 예외 안 터지게 해두긴 했지만 혹시 모르니
            log.warn("optionalUserId: token parse failed: {}", e.getMessage());
            return null;
        }
    }

    /** 응답용 작은 DTO */
    private record FavoriteResponse(boolean favorited, long count) {}
}
