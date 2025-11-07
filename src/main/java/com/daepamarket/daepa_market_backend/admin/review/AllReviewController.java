package com.daepamarket.daepa_market_backend.admin.review;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class AllReviewController {

    private final AllReviewService allReviewService;

    @GetMapping
    public ResponseEntity<List<AllReviewDTO>> getAllReviews() {
        return ResponseEntity.ok(allReviewService.getAllReviews());
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable String reviewId) {
        String prefix = reviewId.substring(0, 1).toUpperCase();
        String numericId = reviewId.replaceAll("[^0-9]", "");
        try {
            allReviewService.deleteReviewWithType(prefix, Long.parseLong(numericId));
            return ResponseEntity.ok("리뷰가 삭제되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
