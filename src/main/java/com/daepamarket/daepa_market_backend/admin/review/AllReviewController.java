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
        allReviewService.deleteReviewWithType(reviewId);
        return ResponseEntity.ok("리뷰가 삭제되었습니다.");
    }
}
