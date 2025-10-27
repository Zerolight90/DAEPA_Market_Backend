package com.daepamarket.daepa_market_backend.favorite;

import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
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
    private final CookieUtil cookieUtil;

    /**
     * ✅ 찜 토글 API
     * - POST /api/favorites/{productId}/toggle
     * - 로그인 필요
     * - 결과로 { favorited, count } 반환
     */
    @PostMapping("/{productId}/toggle")
    public ResponseEntity<?> toggle(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = resolveUserId(request); // 쿠키/토큰에서 사용자 식별
        boolean now = favoriteService.toggle(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(now, count));
    }

    /**
     * ✅ 찜 상태 조회 API
     * - GET /api/favorites/{productId}
     * - 로그인 안 해도 가능 (찜 개수만 표시)
     */
    @GetMapping("/{productId}")
    public ResponseEntity<?> getStatus(HttpServletRequest request, @PathVariable Long productId) {
        Long userId = resolveUserIdNullable(request);
        boolean fav = (userId != null) && favoriteService.isFavorited(userId, productId);
        long count = favoriteService.count(productId);
        return ResponseEntity.ok(new FavoriteResponse(fav, count));
    }

    /**
     * ✅ 내 찜 목록 전체 조회 (마이페이지 - /mypage/like)
     * - GET /api/favorites
     * - 로그인 필요
     * - 응답: [{ pdIdx, pdTitle, pdPrice, imageUrl }, ...]
     */
    @GetMapping
    public ResponseEntity<List<FavoriteItemDTO>> getMyFavorites(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        List<FavoriteItemDTO> list = favoriteService.getMyFavorites(userId);
        return ResponseEntity.ok(list);
    }

    /* ---------------------------------------------------
       아래는 공통 인증 유틸 (ProductController와 동일 로직)
    ----------------------------------------------------*/

    /** ✅ 토큰에서 userId 추출 (로그인 필수 버전) */
    private Long resolveUserId(HttpServletRequest request) {
        String token = resolveAccessToken(request);
        if (token == null || jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        try {
            return Long.valueOf(jwtProvider.getUid(token)); // subject = uIdx
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰");
        }
    }

    /** ✅ 토큰에서 userId 추출 (비로그인 허용 버전) */
    private Long resolveUserIdNullable(HttpServletRequest request) {
        String token = resolveAccessToken(request);
        if (token == null || jwtProvider.isExpired(token)) return null;
        try {
            return Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception ignored) {
            return null;
        }
    }

    /** ✅ 쿠키 또는 Authorization 헤더에서 AccessToken 꺼내기 */
    private String resolveAccessToken(HttpServletRequest request) {
        // 1) 쿠키 우선
        Cookie[] cs = request.getCookies();
        if (cs != null) {
            for (Cookie c : cs) {
                if (CookieUtil.ACCESS.equals(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) return v;
                }
            }
        }
        // 2) Authorization 헤더
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    /** ✅ 응답 DTO (record) */
    private record FavoriteResponse(boolean favorited, long count) {}
}
