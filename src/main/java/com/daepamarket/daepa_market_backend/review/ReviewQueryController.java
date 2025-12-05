// src/main/java/com/daepamarket/daepa_market_backend/review/ReviewQueryController.java
package com.daepamarket.daepa_market_backend.review;

import com.daepamarket.daepa_market_backend.common.dto.PagedResponse;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.review.dto.MyReviewRow;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@RestController
@RequiredArgsConstructor
public class ReviewQueryController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ReviewQueryService reviewQueryService;

    private UserEntity resolveUserFromAuth(String authHeader, HttpServletRequest request) {
        String token = null;
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (request != null && request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> "ACCESS_TOKEN".equalsIgnoreCase(c.getName()) || "accessToken".equalsIgnoreCase(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        if (token == null) return null;

        try {
            String subject = jwtProvider.getUid(token);
            if (subject == null) return null;
            Long uIdx = Long.valueOf(subject);
            return userRepository.findById(uIdx).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** ✅ 내가 받은 후기 */
    @GetMapping("/api/review/received")
    public ResponseEntity<?> pageReceived(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        UserEntity me = resolveUserFromAuth(authHeader, request);
        if (me == null) return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        PagedResponse<MyReviewRow> resp = reviewQueryService.getReceived(me.getUIdx(), page, size);
        return ResponseEntity.ok(resp);
    }

    /** ✅ 내가 작성한 후기 */
    @GetMapping("/api/review/written")
    public ResponseEntity<?> pageWritten(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        UserEntity me = resolveUserFromAuth(authHeader, request);
        if (me == null) return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        PagedResponse<MyReviewRow> resp = reviewQueryService.getWritten(me.getUIdx(), page, size);
        return ResponseEntity.ok(resp);
    }

    /** ✅ 특정 유저(판매자 등)가 받은 후기 (공개 페이지) */
    @GetMapping("/api/review/user/{sellerId}")
    public ResponseEntity<PagedResponse<MyReviewRow>> getReviewsBySeller(
            @PathVariable Long sellerId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewQueryService.pageReceivedByUser(sellerId, page, size));
    }
}
