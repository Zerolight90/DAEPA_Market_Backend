package com.daepamarket.daepa_market_backend.deal;

import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.deal.DealSafeDTO;
import com.daepamarket.daepa_market_backend.domain.deal.DealSellHistoryDTO;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
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
}

