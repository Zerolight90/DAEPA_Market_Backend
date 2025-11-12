// src/main/java/com/daepamarket/daepa_market_backend/review/ReviewController.java
package com.daepamarket.daepa_market_backend.review;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.review.dto.ReviewCreateRequest;
import com.daepamarket.daepa_market_backend.review.dto.ReviewUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ReviewService reviewService;

    /**
     * 후기 작성 (기존에 있던 것)
     */
    @PostMapping("/api/reviews")
    public ResponseEntity<?> createReview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ReviewCreateRequest dto
    ) {

        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body("Authorization 헤더가 없습니다.");
        }
        String token = authHeader.substring(7).trim();

        String subject = jwtProvider.getUid(token);
        if (subject == null) {
            return ResponseEntity.status(401).body("토큰에서 uid를 읽을 수 없습니다.");
        }

        // subject 가 숫자일 수도 있고, 문자열 uid 일 수도 있으니까 둘 다 시도
        UserEntity writer;
        try {
            Long uIdx = Long.valueOf(subject);
            writer = userRepository.findById(uIdx).orElse(null);
        } catch (NumberFormatException e) {
            writer = userRepository.findByUid(subject).orElse(null);
        }

        if (writer == null) {
            return ResponseEntity.badRequest()
                    .body("사용자를 찾을 수 없습니다. 토큰 subject=" + subject);
        }

        // 디버그
        System.out.println("[REVIEW CTRL] /api/reviews 호출됨 by user=" + writer.getUIdx());

        try {
            Long reviewId = reviewService.createReview(writer.getUIdx(), dto);
            return ResponseEntity.ok(reviewId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            // 중복리뷰일 때 여기로 온다
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ✅ 추가: 이 거래에 내가 이 타입으로 이미 썼는지 확인
     * 예) /api/review/exists?dealId=419&reType=SELLER
     */
    @GetMapping("/api/review/exists")
    public ResponseEntity<?> existsReview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("dealId") Long dealId,
            @RequestParam("reType") String reType
    ) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body("Authorization 헤더가 없습니다.");
        }
        String token = authHeader.substring(7).trim();

        String subject = jwtProvider.getUid(token);
        if (subject == null) {
            return ResponseEntity.status(401).body("토큰에서 uid를 읽을 수 없습니다.");
        }

        // 토큰으로 사용자 찾기
        UserEntity writer;
        try {
            Long uIdx = Long.valueOf(subject);
            writer = userRepository.findById(uIdx).orElse(null);
        } catch (NumberFormatException e) {
            writer = userRepository.findByUid(subject).orElse(null);
        }

        if (writer == null) {
            return ResponseEntity.badRequest()
                    .body("사용자를 찾을 수 없습니다. 토큰 subject=" + subject);
        }

        boolean exists = reviewService.existsReview(writer.getUIdx(), dealId, reType);

        return ResponseEntity.ok(Map.of("exists", exists));
    }
    @PutMapping("/api/reviews/{reIdx}")
    public ResponseEntity<?> updateReview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long reIdx,
            @RequestBody ReviewUpdateRequest dto
    ) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body("Authorization 헤더가 없습니다.");
        }
        String token = authHeader.substring(7).trim();
        String subject = jwtProvider.getUid(token);
        if (subject == null) return ResponseEntity.status(401).body("토큰에서 uid를 읽을 수 없습니다.");

        UserEntity writer;
        try {
            Long uIdx = Long.valueOf(subject);
            writer = userRepository.findById(uIdx).orElse(null);
        } catch (NumberFormatException e) {
            writer = userRepository.findByUid(subject).orElse(null);
        }
        if (writer == null) return ResponseEntity.badRequest().body("사용자를 찾을 수 없습니다.");

        try {
            reviewService.updateReview(writer.getUIdx(), reIdx, dto);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
