package com.daepamarket.daepa_market_backend.review;

import com.daepamarket.daepa_market_backend.common.dto.PagedResponse;
import com.daepamarket.daepa_market_backend.domain.review.ReviewRepository;
import com.daepamarket.daepa_market_backend.review.dto.MyReviewRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;   // ✅ 정확한 import
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;

    public PagedResponse<MyReviewRow> getReceived(Long uid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);  // ✅ 명시적 타입
        Page<MyReviewRow> p = reviewRepository.pageReceivedByUser(uid, pageable);

        return new PagedResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalPages(),
                p.getTotalElements()
        );
    }

    public PagedResponse<MyReviewRow> getWritten(Long uid, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);  // ✅ 동일
        Page<MyReviewRow> p = reviewRepository.pageWrittenByUser(uid, pageable);

        return new PagedResponse<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalPages(),
                p.getTotalElements()
        );
    }
}
