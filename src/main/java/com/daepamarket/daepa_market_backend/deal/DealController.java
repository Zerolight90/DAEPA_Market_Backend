package com.daepamarket.daepa_market_backend.deal;


import com.daepamarket.daepa_market_backend.domain.deal.DealSellHistoryDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deal")
public class DealController {

    private final DealService dealService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    // 안전결제 정산 내역 (내가 판 것)
    @GetMapping("/safe")
    public ResponseEntity<?> getMySettlements(HttpServletRequest request) {
        return dealService.getMySettlements(request);
    }

    @GetMapping("/mySell")
    public ResponseEntity<?> getMySell(HttpServletRequest request) {
        try {
            // 1) Authorization 헤더 꺼내기
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("토큰이 없습니다.");
            }

            // 2) Bearer 잘라내기
            String token = auth.substring(7);

            // 3) 토큰 만료 확인
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            // 4) 토큰에서 userId 뽑기
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            // 5) DB에 진짜 존재하는지 체크
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
            }

            // 6) 판매자 idx = 토큰 주인인 거래만 조회
            List<DealSellHistoryDTO> list = dealService.getMySellHistory(userId);

            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }
}
