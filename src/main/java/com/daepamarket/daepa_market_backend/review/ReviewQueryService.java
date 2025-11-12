// src/main/java/com/daepamarket/daepa_market_backend/review/ReviewQueryService.java
package com.daepamarket.daepa_market_backend.review;

import com.daepamarket.daepa_market_backend.common.dto.PagedResponse;
import com.daepamarket.daepa_market_backend.domain.review.ReviewRepository;
import com.daepamarket.daepa_market_backend.review.dto.MyReviewRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;

    /** ✅ 내가 받은 후기 (마이페이지) */
    public PagedResponse<MyReviewRow> getReceived(Long uid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MyReviewRow> p = reviewRepository.pageReceivedByUser(uid, pageable);
        return new PagedResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalPages(), p.getTotalElements());
    }

    /** ✅ 내가 작성한 후기 (마이페이지) */
    public PagedResponse<MyReviewRow> getWritten(Long uid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MyReviewRow> p = reviewRepository.pageWrittenByUser(uid, pageable);
        return new PagedResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalPages(), p.getTotalElements());
    }

    /** ✅ 특정 유저(판매자 등)가 받은 후기 (공개 페이지) */
    public PagedResponse<MyReviewRow> pageReceivedByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MyReviewRow> p = reviewRepository.pageReceivedByTargetUser(userId, pageable);
        return new PagedResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalPages(), p.getTotalElements());
    }
}
