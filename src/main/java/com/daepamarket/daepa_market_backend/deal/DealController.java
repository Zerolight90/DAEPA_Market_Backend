package com.daepamarket.daepa_market_backend.deal;


import com.daepamarket.daepa_market_backend.domain.deal.DealSellHistoryDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.pay.PayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deal")
public class DealController {

    private final DealService dealService;
    private final PayService payService; // ✅ PayService 주입
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    /**
     * ✅ [신규] 구매 확정 API
     * @param dIdx 확정할 거래 ID
     * @param request 사용자 인증을 위한 HttpServletRequest
     */
    @PostMapping("/{dIdx}/confirm")
    public ResponseEntity<?> confirmPurchase(
            @PathVariable Long dIdx,
            HttpServletRequest request) {
        try {
            // 1. 토큰에서 사용자 ID 추출 (기존 로직 활용)
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "인증 토큰이 없습니다."));
            }
            String token = auth.substring(7);
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "토큰이 만료되었습니다."));
            }
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            // 2. 서비스 로직 호출
            payService.finalizePurchase(dIdx, userId);

            return ResponseEntity.ok(Map.of("message", "구매가 성공적으로 확정되었습니다. 판매자에게 정산이 완료됩니다."));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            // 서버 오류 로깅 (실제 운영시 중요)
            // log.error("구매 확정 처리 중 오류 발생: dealId={}, userId={}", dIdx, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "구매 확정 처리 중 서버 오류가 발생했습니다."));
        }
    }

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
