package com.daepamarket.daepa_market_backend.deal;

import com.daepamarket.daepa_market_backend.domain.deal.DealBuyHistoryDTO;
import com.daepamarket.daepa_market_backend.domain.deal.DealSellHistoryDTO;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
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
    private final PayService payService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final CookieUtil cookieUtil;

    /**
     * ✅ [신규] 구매 확정 API
     */
    @PostMapping("/{dealId}/confirm")
    public ResponseEntity<?> confirmPurchase(
            @PathVariable Long dealId,
            HttpServletRequest request) {
        try {
            String token = cookieUtil.getAccessTokenFromCookie(request);
            if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
            }
            Long uIdx = Long.valueOf(jwtProvider.getUid(token));

            payService.finalizePurchase(dealId, uIdx);
            dealService.buyerMannerUp(uIdx);

            return ResponseEntity.ok(Map.of("message", "구매가 성공적으로 확정되었습니다. 판매자에게 정산이 완료됩니다."));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "서버 오류가 발생했습니다."));
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
            String token = cookieUtil.getAccessTokenFromCookie(request);
            if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));

            UserEntity user = userRepository.findById(uIdx).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
            }

            List<DealSellHistoryDTO> list = dealService.getMySellHistory(uIdx);
            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }

    @GetMapping("/myBuy")
    public ResponseEntity<?> getMyBuys(HttpServletRequest request) {
        try {
            String token = cookieUtil.getAccessTokenFromCookie(request);
            if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            List<DealBuyHistoryDTO> list = dealService.getMyBuyHistory(uIdx);
            return ResponseEntity.ok(list);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    // 구매내역에서 구매확정 버튼
    @PatchMapping("/{dealId}/buy-confirm")
    public ResponseEntity<?> confirmBuy(
            @PathVariable Long dealId,
            HttpServletRequest request
    ) {
        try {
            String token = cookieUtil.getAccessTokenFromCookie(request);
            if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            dealService.confirmBuy(dealId, uIdx);

            return ResponseEntity.ok("구매확정 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    @GetMapping("/safe/count")
    public ResponseEntity<Map<String, Long>> getSettlementCount(
            @RequestParam Long uIdx
    ) {
        long count = dealService.getSettlementCount(uIdx);
        return ResponseEntity.ok(Map.of("count", count));
    }
}