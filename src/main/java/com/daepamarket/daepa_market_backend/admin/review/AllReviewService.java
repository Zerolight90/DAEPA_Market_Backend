package com.daepamarket.daepa_market_backend.admin.review;

import com.daepamarket.daepa_market_backend.domain.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AllReviewService {

    private final ReviewRepository reviewRepository;

    public List<AllReviewDTO> getAllReviews() {
        return reviewRepository.findAllReviewRows();
    }

    /**
     * 프론트에서 "S-10" / "B-3" 이런 식으로 보내면
     * 실제 숫자만 뽑아서 삭제
     */
    @Transactional
    public void deleteReviewWithType(String prefixedId) {
        // 예: "S-12" → "12"
        String numeric = prefixedId.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) {
            throw new RuntimeException("잘못된 리뷰 ID 형식입니다: " + prefixedId);
        }
        Long realId = Long.parseLong(numeric);

        if (!reviewRepository.existsById(realId)) {
            throw new RuntimeException("해당 리뷰가 존재하지 않습니다: " + prefixedId);
        }
        reviewRepository.deleteById(realId);
    }
}
