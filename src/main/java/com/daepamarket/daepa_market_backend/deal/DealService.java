package com.daepamarket.daepa_market_backend.deal;

import com.daepamarket.daepa_market_backend.domain.deal.*;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;
    private final JwtProvider jwtProvider;

    public ResponseEntity<?> getMySettlements(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("토큰이 없습니다.");
            }

            String token = auth.substring(7);
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));

            List<DealSafeDTO> list = dealRepository.findSettlementsBySeller(uIdx);

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    public List<DealSellHistoryDTO> getMySellHistory(Long sellerIdx) {
        return dealRepository.findSellHistoryBySeller(sellerIdx);
    }

    public List<DealBuyHistoryDTO> getMyBuyHistory(Long sellerIdx) {
        return dealRepository.findBuyHistoryByBuyer(sellerIdx);
    }

    // 구매내역에서 구매확정 버튼
    @Transactional
    public void confirmBuy(Long dealId, Long buyerId) {
        DealEntity deal = dealRepository.findByIdAndBuyer(dealId, buyerId)
                .orElseThrow(() -> new IllegalArgumentException("거래가 없거나 내 거래가 아닙니다."));

        // 이미 구매확정 되어 있으면 그냥 끝
        if (deal.getDBuy() != null && deal.getDBuy() == 1L) {
            // 그래도 d_status 체크 한 번 더
            if (deal.getDSell() != null && deal.getDSell() == 1L) {
                deal.setDStatus(1L);
            }
            return;
        }

        // 1) 나(구매자)가 확정
        deal.setDBuy(1L);

        // 2) 판매자도 이미 확정(d_sell=1) 했으면 거래 완료로
        if (deal.getDSell() != null && deal.getDSell() == 1L) {
            deal.setDStatus(1L);
        }
    }

}

