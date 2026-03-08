package com.daepamarket.daepa_market_backend.pay;

import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;
    private final RestTemplate restTemplate;

    @Value("${TOSS_SECRET_KEY}")
    private String tossSecretKey;

    @Value("${app.front-url}")
    private String frontUrl;

    // ✅ 만능 열쇠 헬퍼 메서드
    private String extractToken(HttpServletRequest request) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            return null;
        }
        return token;
    }

    // Toss Payments가 성공 시 호출하는 엔드포인트
    @GetMapping("/api/charge/success")
    public void handleChargeSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse,
            HttpServletRequest request
    ) throws IOException {
        String token = extractToken(request);
        if (token == null) {
            handleAuthError(httpServletResponse, "로그인이 필요하거나 토큰이 만료되었습니다.");
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            handleAuthError(httpServletResponse, "유효하지 않은 토큰입니다.");
            return;
        }

        try {
            payService.confirmPointCharge(paymentKey, orderId, amount, userId);
            String redirectUrl = frontUrl + "/payCharge/success?amount=" + amount;
            httpServletResponse.sendRedirect(redirectUrl);
        } catch (Exception e) {
            String errorMsg = e.getMessage().replace(" ", "+");
            String redirectUrl = frontUrl + "/payCharge/fail?message=" + errorMsg;
            httpServletResponse.sendRedirect(redirectUrl);
        }
    }

    // --- 페이 잔액 조회 API ---
    @GetMapping("/api/pay/balance")
    public ResponseEntity<?> getPayBalance(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요하거나 토큰이 만료되었습니다."));
        }

        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }

        try {
            long balance = payService.getCurrentBalance(userId);
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (Exception e) {
            System.err.println("잔액 조회 중 서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "잔액 조회 중 오류가 발생했습니다."));
        }
    }

    // --- 페이로 상품 구매 API ---
    @PostMapping("/api/pay/purchase-with-points")
    public ResponseEntity<?> purchaseWithPoints(
            @RequestBody PayRequestDTO payRequestDTO,
            HttpServletRequest request) {

        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요하거나 토큰이 만료되었습니다."));
        }

        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }

        try {
            long remainingBalance = payService.processPurchaseWithPoints(
                    userId,
                    payRequestDTO.getItemId(),
                    payRequestDTO.getQty(),
                    payRequestDTO.getAmount()
            );
            return ResponseEntity.ok(Map.of("message", "결제 완료", "remainingBalance", remainingBalance));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("페이 결제 처리 중 서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "페이 결제 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/api/pay/confirm")
    public ResponseEntity<?> confirmPayment(
            @RequestBody PaymentConfirmDto confirmDto,
            HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
            }
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            String url = "https://api.tosspayments.com/v1/payments/confirm";
            HttpHeaders headers = new HttpHeaders();
            String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes());
            headers.setBasicAuth(encodedKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("paymentKey", confirmDto.getPaymentKey());
            bodyMap.put("orderId", confirmDto.getOrderId());
            bodyMap.put("amount", confirmDto.getAmount());

            HttpEntity<Map<String, Object>> tossRequestEntity = new HttpEntity<>(bodyMap, headers);
            restTemplate.postForEntity(url, tossRequestEntity, String.class);

            payService.confirmProductPurchase(confirmDto.getPaymentKey(), confirmDto.getOrderId(), confirmDto.getAmount(), userId);
            return ResponseEntity.ok(Map.of("message", "결제가 성공적으로 처리되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "결제 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/api/pay/success")
    public void handlePaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse,
            HttpServletRequest request) throws IOException { 
        try {
            String token = extractToken(request);
            if (token == null) {
                handleAuthError(httpServletResponse, "유효하지 않은 토큰입니다.");
                return;
            }
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            String url = "https://api.tosspayments.com/v1/payments/confirm";
            HttpHeaders headers = new HttpHeaders();
            String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes());
            headers.setBasicAuth(encodedKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("paymentKey", paymentKey);
            bodyMap.put("orderId", orderId);
            bodyMap.put("amount", amount);

            HttpEntity<Map<String, Object>> tossRequestEntity = new HttpEntity<>(bodyMap, headers);

            try {
                restTemplate.postForEntity(url, tossRequestEntity, String.class);
            } catch (Exception e) {
                System.err.println("Toss Payments 승인 API 호출 실패: " + e.getMessage());
                throw new RuntimeException("결제 승인에 실패했습니다. (API 오류)");
            }

            payService.confirmProductPurchase(paymentKey, orderId, amount, userId);
            String redirectUrl = frontUrl + "/pay/success?amount=" + amount + "&orderId=" + orderId;
            httpServletResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            String errorMsg = java.net.URLEncoder.encode(e.getMessage(), "UTF-8");
            String redirectUrl = frontUrl + "/pay/fail?message=" + errorMsg + "&orderId=" + orderId;
            httpServletResponse.sendRedirect(redirectUrl);
        }
    }

    @GetMapping("/api/secPay/success")
    public void handleSecPaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse,
            HttpServletRequest request) throws IOException {

        try {
            String token = extractToken(request);
            if (token == null) {
                handleAuthError(httpServletResponse, "유효하지 않은 토큰입니다.");
                return;
            }
            Long userId = Long.valueOf(jwtProvider.getUid(token));

            String url = "https://api.tosspayments.com/v1/payments/confirm";
            HttpHeaders headers = new HttpHeaders();
            String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes());
            headers.setBasicAuth(encodedKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("paymentKey", paymentKey);
            bodyMap.put("orderId", orderId);
            bodyMap.put("amount", amount);

            HttpEntity<Map<String, Object>> tossRequestEntity = new HttpEntity<>(bodyMap, headers);

            try {
                restTemplate.postForEntity(url, tossRequestEntity, String.class);
            } catch (Exception e) {
                System.err.println("Toss Payments 승인 API 호출 실패: " + e.getMessage());
                throw new RuntimeException("결제 승인에 실패했습니다. (API 오류)");
            }

            payService.confirmProductSecPurchase(paymentKey, orderId, amount, userId);
            String redirectUrl = frontUrl + "/pay/sec/success?amount=" + amount + "&orderId=" + orderId;
            httpServletResponse.sendRedirect(redirectUrl);

        } catch (Exception e) {
            String errorMsg = java.net.URLEncoder.encode(e.getMessage(), "UTF-8");
            String redirectUrl = frontUrl + "/pay/sec/fail?message=" + errorMsg + "&orderId=" + orderId;
            httpServletResponse.sendRedirect(redirectUrl);
        }
    }

    @PostMapping("/api/{dIdx}/payCancel")
    public ResponseEntity<?> handlePayCancel(
            @PathVariable Long dIdx,
            @RequestBody CancelRequestDto cancelDto,
            HttpServletRequest request
    ) {
        Long userId;
        try {
            String token = extractToken(request);
            if (token == null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."); }
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }

        try {
            payService.cancelProductPurchase(dIdx, userId, cancelDto.getCancelReason());
            return ResponseEntity.ok(Map.of("message", "결제가 성공적으로 취소되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("결제 취소 처리 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "결제 취소 중 오류가 발생했습니다."));
        }
    }

    @Getter
    @NoArgsConstructor
    static class CancelRequestDto {
        private String cancelReason;
    }

    private void handleAuthError(HttpServletResponse response, String message) throws IOException {
        String errorMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(frontUrl + "/login?error=" + errorMsg);
    }
}