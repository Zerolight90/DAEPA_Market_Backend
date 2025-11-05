package com.daepamarket.daepa_market_backend.admin.review;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AllReviewService {

    private final SaleReviewRepository saleReviewRepository;
    private final BuyReviewRepository buyReviewRepository;

    public List<AllReviewDTO> getAllReviews() {
        List<AllReviewDTO> results = new ArrayList<>();

        // 판매자 리뷰
        results.addAll(saleReviewRepository.findAllSaleReviewRows());

        // 구매자 리뷰
        results.addAll(buyReviewRepository.findAllBuyReviewRows());

        return results;
    }

    @Transactional
    public void deleteReview(Long id) {
        // 구매/판매 리뷰 테이블 모두에서 삭제 대응
        if (saleReviewRepository.existsById(id)) {
            saleReviewRepository.deleteById(id);
        } else if (buyReviewRepository.existsById(id)) {
            buyReviewRepository.deleteById(id);
        } else {
            throw new RuntimeException("해당 리뷰가 존재하지 않습니다: " + id);
        }
    }
}
