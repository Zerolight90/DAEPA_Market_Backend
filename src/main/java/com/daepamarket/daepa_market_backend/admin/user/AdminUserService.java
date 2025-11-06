package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.admin.review.SaleReviewDTO;
import com.daepamarket.daepa_market_backend.admin.review.SaleReviewRepository;
import com.daepamarket.daepa_market_backend.domain.naga.NagaRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final NagaRepository nagaRepository;
    private final SaleReviewRepository saleReviewRepository;


    public UserDetailDTO getUserDetail(Long uIdx) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 구매/판매 거래 조회
        List<DealEntity> buys = dealRepository.findByBuyer_uIdx(uIdx);
        List<DealEntity> sells = dealRepository.findBySeller_uIdx(uIdx);

        // 하나의 거래 리스트로 합치기
        List<TradeHistoryDTO> history = new ArrayList<>();
        buys.forEach(d -> history.add(TradeHistoryDTO.fromEntity(d, "BUY")));
        sells.forEach(d -> history.add(TradeHistoryDTO.fromEntity(d, "SELL")));

        // 최신순 정렬
        history.sort((a, b) -> {
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        // 유저 DTO 매핑 + 거래내역 넣기
        UserDetailDTO dto = UserDetailDTO.fromEntity(user);
        dto.setTradeHistory(history);

        // 신고 내역 조회
        List<ReportHistoryDTO> reports = nagaRepository.findReportsByUserId(uIdx);
        dto.setReportHistory(reports);

        return dto;
    }

    @Transactional
    public void updateManner(Long uIdx, Double manner) {
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));
        user.setUManner(manner);
        userRepository.save(user);
    }

    // 판매 후기 조회
    public List<SaleReviewDTO> getUserSaleReviews(Long userId) {
        return saleReviewRepository.findSaleReviewsBySeller(userId);
    }

}