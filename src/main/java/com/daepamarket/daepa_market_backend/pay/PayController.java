package com.daepamarket.daepa_market_backend.pay;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    // Toss Payments가 성공 시 호출하는 엔드포인트
    @GetMapping("/api/charge/success")
    public void handleChargeSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse) throws IOException {
        try {
            // 실제 로직은 Service 계층에 위임
            payService.confirmPointCharge(paymentKey, orderId, amount);
            // 성공 시 사용자에게 보여줄 페이지로 리다이렉트
            String redirectUrl = "http://localhost:3000/payCharge/success?amount=" + amount;
            httpServletResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // 실패 시 에러 페이지로 (실제 페이지 작성 후 변경!)
            String errorMsg = e.getMessage().replace(" ", "+"); // URL 인코딩 필요
            String redirectUrl = "http://localhost:3000/payCharge/fail?message=" + errorMsg;
            httpServletResponse.sendRedirect(redirectUrl);
        }
    }

    // --- 페이 잔액 조회 API ---
    @GetMapping("/balance")
    public ResponseEntity<?> getPayBalance(Authentication authentication) { // ✅ Authentication 파라미터 추가
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            // ✅ Authentication 객체에서 사용자 ID (u_idx) 추출 (Principal이 ID 문자열이라고 가정)
            Long userId = Long.parseLong(authentication.getName());

            long balance = payService.getCurrentBalance(userId);
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "사용자 ID 형식이 잘못되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ 페이 구매 요청 DTO (새로 생성)
    @Getter
    @NoArgsConstructor
    class PurchaseRequestDto {
        private Long itemId;
        private int qty;
        private Long amount;
    }

    // --- 페이로 상품 구매 API ---
    @PostMapping("/purchase-with-points")
    public ResponseEntity<?> purchaseWithPoints(
            @RequestBody PurchaseRequestDto requestDto, // ✅ 요청 DTO 사용
            Authentication authentication) { // ✅ Authentication 파라미터 추가

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다."));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());

            // ✅ 서비스 호출 시 사용자 ID 전달
            long remainingBalance = payService.processPurchaseWithPoints(
                    userId,
                    requestDto.getItemId(),
                    requestDto.getQty(),
                    requestDto.getAmount()
            );

            return ResponseEntity.ok(Map.of("message", "결제 완료", "remainingBalance", remainingBalance));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "사용자 ID 형식이 잘못되었습니다."));
        } catch (IllegalArgumentException e) { // 잔액 부족 등 서비스 예외 처리
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/pay/success")
    public void handlePaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse) throws IOException {
        try {
            // 실제 로직은 Service 계층에 위임
            payService.confirmProductPurchase(paymentKey, orderId, amount);
            // 성공 시 사용자에게 보여줄 페이지로 리다이렉트
            String redirectUrl = "http://localhost:3000/pay/success?amount=" + amount + "&orderId=" + orderId;
            httpServletResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // 실패 시 에러 페이지로 (실제 페이지 작성 후 변경!)
            String errorMsg = e.getMessage().replace(" ", "+"); // URL 인코딩 필요
            String redirectUrl = "http://localhost:3000/pay/fail?message=" + errorMsg;
            httpServletResponse.sendRedirect(redirectUrl);
        }
    }

    @GetMapping("/api/secPay/success")
    public void handleSecPaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse) throws IOException {
        try {
            // 실제 로직은 Service 계층에 위임
            payService.confirmProductPurchase(paymentKey, orderId, amount);
            // 성공 시 사용자에게 보여줄 페이지로 리다이렉트
            String redirectUrl = "http://localhost:3000/pay/sec/success?amount=" + amount + "&orderId=" + orderId;
            httpServletResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // 실패 시 에러 페이지로 (실제 페이지 작성 후 변경!)
            String errorMsg = e.getMessage().replace(" ", "+"); // URL 인코딩 필요
            String redirectUrl = "http://localhost:3000/pay/sec/fail?message=" + errorMsg;
            httpServletResponse.sendRedirect(redirectUrl);
        }
    }
}