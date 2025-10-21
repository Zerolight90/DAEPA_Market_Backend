package com.daepamarket.daepa_market_backend.pay.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.daepamarket.daepa_market_backend.pay.service.PayService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    // Toss Payments가 성공 시 호출하는 엔드포인트
    @GetMapping("/api/pay/success")
    public ResponseEntity<String> handlePaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount
    ) {
        try {
            // 실제 로직은 Service 계층에 위임
            payService.confirmPaymentAndUpdateDb(paymentKey, orderId, amount);
            // 성공 시 사용자에게 보여줄 페이지로 리다이렉트 (예시이므로 실제 페이지 작성 후 변경!)
            return ResponseEntity.ok("충전이 완료되었습니다.");
            // 실제로는 response.sendRedirect("/charge/success-page"); 와 같이 사용
        } catch (Exception e) {
            // 실패 시 에러 페이지로 (실제 페이지 작성 후 변경!)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}