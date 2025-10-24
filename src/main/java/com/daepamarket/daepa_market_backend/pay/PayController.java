package com.daepamarket.daepa_market_backend.pay;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.io.IOException;

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
            payService.confirmPaymentAndUpdateDb(paymentKey, orderId, amount);
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

    @GetMapping("/api/pay/success")
    public void handlePaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse) throws IOException {
        try {
            // 실제 로직은 Service 계층에 위임
            payService.confirmPaymentAndUpdateDb(paymentKey, orderId, amount);
            // 성공 시 사용자에게 보여줄 페이지로 리다이렉트
            String redirectUrl = "http://localhost:3000/pay/success?amount=" + amount;
            httpServletResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            // 실패 시 에러 페이지로 (실제 페이지 작성 후 변경!)
            String errorMsg = e.getMessage().replace(" ", "+"); // URL 인코딩 필요
            String redirectUrl = "http://localhost:3000/pay/fail?message=" + errorMsg;
            httpServletResponse.sendRedirect(redirectUrl);
        }
    }
}