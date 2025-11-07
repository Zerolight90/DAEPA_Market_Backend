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
    public void deleteReviewWithType(String type, Long id) {
        if ("S".equalsIgnoreCase(type)) {
            if (!saleReviewRepository.existsById(id)) {
                throw new RuntimeException("해당 판매후기가 존재하지 않습니다: " + id);
            }
            saleReviewRepository.deleteById(id);
        } else if ("B".equalsIgnoreCase(type)) {
            if (!buyReviewRepository.existsById(id)) {
                throw new RuntimeException("해당 구매후기가 존재하지 않습니다: " + id);
            }
            buyReviewRepository.deleteById(id);
        } else {
            throw new RuntimeException("유효하지 않은 리뷰 타입입니다: " + type);
        }
    }
}
