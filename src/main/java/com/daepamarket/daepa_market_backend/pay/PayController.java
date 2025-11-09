package com.daepamarket.daepa_market_backend.pay;

import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    // Toss Payments가 성공 시 호출하는 엔드포인트
    @GetMapping("/api/charge/success")
    public void handleChargeSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse,
            HttpServletRequest request
    ) throws IOException {
        // ===== 1. 토큰 추출 및 검증 =====
        String token = resolveAccessToken(request);
        if (token == null) {
            handleAuthError(httpServletResponse, "로그인이 필요합니다 (토큰 없음).");
            return;
        }
        if (jwtProvider.isExpired(token)) {
            handleAuthError(httpServletResponse, "토큰이 만료되었습니다.");
            return;
        }

        // ===== 2. 토큰에서 사용자 ID 추출 =====
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            handleAuthError(httpServletResponse, "유효하지 않은 토큰입니다.");
            return;
        }
        // =======================================================

        try {
            // 실제 로직은 Service 계층에 위임
            payService.confirmPointCharge(paymentKey, orderId, amount, userId);
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
    @GetMapping("/api/pay/balance")
    public ResponseEntity<?> getPayBalance(HttpServletRequest request) {
        // ===== 1. 토큰 추출 및 검증 (ProductController 방식) =====
        String token = resolveAccessToken(request); // 토큰 추출
        System.out.println("### 전달받은 토큰: " + token); // ✅ 로그 추가!
        if (token == null) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다 (토큰 없음)."));
        }
        if (jwtProvider.isExpired(token)) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "토큰이 만료되었습니다."));
        }

        // ===== 2. 토큰에서 사용자 ID 추출 =====
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token)); // 토큰 subject가 uIdx (문자열)라고 가정
            System.out.println("### 추출된 User ID: " + userId); // ✅ 성공 시 로그 추가!
        } catch (Exception e) {
            // 401 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }
        // =======================================================

        // ===== 3. 비즈니스 로직 호출 =====
        try {
            long balance = payService.getCurrentBalance(userId); // 추출한 userId 사용
            return ResponseEntity.ok(Map.of("balance", balance));
        } catch (Exception e) {
            // 서비스 로직에서 발생한 예외 처리 (e.g., UserNotFound)
            System.err.println("잔액 조회 중 서버 오류: " + e.getMessage()); // 로그 추가 권장
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "잔액 조회 중 오류가 발생했습니다."));
        }
    }

    // --- 토큰 추출 헬퍼 메소드 ---
    private String resolveAccessToken(HttpServletRequest request) {
        // 1) 쿠키 우선
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                // ✅ CookieUtil.ACCESS (쿠키 이름) 확인!
                if (CookieUtil.ACCESS.equals(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) return v;
                }
            }
        }
        // 2) Authorization: Bearer 헤더
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null; // 토큰 못 찾음
    }

    // --- 페이로 상품 구매 API ---
    @PostMapping("/api/pay/purchase-with-points")
    public ResponseEntity<?> purchaseWithPoints(
            @RequestBody PayRequestDTO payRequestDTO, // ✅ 요청 DTO 사용
            HttpServletRequest request) {

        // ===== 1. 토큰 추출 및 검증 =====
        String token = resolveAccessToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "로그인이 필요합니다 (토큰 없음)."));
        }
        if (jwtProvider.isExpired(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "토큰이 만료되었습니다."));
        }

        // ===== 2. 토큰에서 사용자 ID 추출 =====
        Long userId;
        try {
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }
        // ===================================

        // ===== 3. 비즈니스 로직 호출 =====
        try {
            long remainingBalance = payService.processPurchaseWithPoints(
                    userId, // 추출한 userId 사용
                    payRequestDTO.getItemId(),
                    payRequestDTO.getQty(),
                    payRequestDTO.getAmount()
            );
            return ResponseEntity.ok(Map.of("message", "결제 완료", "remainingBalance", remainingBalance));
        } catch (IllegalArgumentException e) { // 잔액 부족, 금액 불일치 등
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("페이 결제 처리 중 서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "페이 결제 처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/api/pay/success")
    public void handlePaymentSuccess(
            @RequestParam String paymentKey,
            @RequestParam String orderId,
            @RequestParam Long amount,
            HttpServletResponse httpServletResponse,
            HttpServletRequest request) throws IOException { // ✅ HttpServletRequest 추가
        try {
            // ===== 1. 토큰 추출 및 검증 =====
            String token = resolveAccessToken(request);
            if (token == null) {
                handleAuthError(httpServletResponse, "로그인이 필요합니다 (토큰 없음).");
                return;
            }
            if (jwtProvider.isExpired(token)) {
                handleAuthError(httpServletResponse, "토큰이 만료되었습니다.");
                return;
            }

            // ===== 2. 토큰에서 사용자 ID 추출 =====
            Long userId;
            try {
                userId = Long.valueOf(jwtProvider.getUid(token));
            } catch (Exception e) {
                handleAuthError(httpServletResponse, "유효하지 않은 토큰입니다.");
                return;
            }
            // =======================================================

            // 실제 로직은 Service 계층에 위임 (✅ userId 전달)
            payService.confirmProductPurchase(paymentKey, orderId, amount, userId);

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
            HttpServletResponse httpServletResponse,
            HttpServletRequest request) throws IOException { // ✅ HttpServletRequest 추가
        try {
            // ===== 1. 토큰 추출 및 검증 =====
            String token = resolveAccessToken(request);
            if (token == null) {
                handleAuthError(httpServletResponse, "로그인이 필요합니다 (토큰 없음).");
                return;
            }
            if (jwtProvider.isExpired(token)) {
                handleAuthError(httpServletResponse, "토큰이 만료되었습니다.");
                return;
            }

            // ===== 2. 토큰에서 사용자 ID 추출 =====
            Long userId;
            try {
                userId = Long.valueOf(jwtProvider.getUid(token));
            } catch (Exception e) {
                handleAuthError(httpServletResponse, "유효하지 않은 토큰입니다.");
                return;
            }
            // =======================================================

            // 실제 로직은 Service 계층에 위임 (✅ userId 전달)
            payService.confirmProductSecPurchase(paymentKey, orderId, amount, userId);
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

    /**
     * ✅ [신규] 상품 구매 취소 (환불) API
     * @param dIdx 취소할 거래(Deal)의 ID
     * @param cancelDto 취소 사유가 담긴 DTO
     * @param request 토큰 검증을 위한 HttpServletRequest
     */
    @PostMapping("/api/{dIdx}/payCancel")
    public ResponseEntity<?> handlePayCancel(
            @PathVariable Long dIdx,
            @RequestBody CancelRequestDto cancelDto, // {"cancelReason": "사유"}
            HttpServletRequest request
    ) {
        Long userId;
        try {
            // ===== 1. 토큰 검증 및 사용자 ID 추출 =====
            String token = resolveAccessToken(request);
            if (token == null) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."); }
            if (jwtProvider.isExpired(token)) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."); }
            userId = Long.valueOf(jwtProvider.getUid(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "유효하지 않은 토큰입니다."));
        }

        try {
            // ===== 2. 서비스 로직 호출 =====
            payService.cancelProductPurchase(dIdx, userId, cancelDto.getCancelReason());
            return ResponseEntity.ok(Map.of("message", "결제가 성공적으로 취소되었습니다."));

        } catch (IllegalStateException e) { // 이미 취소된 경우 등
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) { // 권한 없는 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) { // 기타 서버 오류
            System.err.println("결제 취소 처리 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "결제 취소 중 오류가 발생했습니다."));
        }
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create("https://api.tosspayments.com/v1/payments/tviva20251031143331MUp15/cancel"))
//                .header("Authorization", "Basic dGVzdF9za196WExrS0V5cE5BcldtbzUwblgzbG1lYXhZRzVSOg==")
//                .header("Content-Type", "application/json")
//                .method("POST", HttpRequest.BodyPublishers.ofString("{\"cancelReason\":\"구매자 변심\"}"))
//                .build();
//        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
//        System.out.println(response.body());


    }


    @Getter
    @NoArgsConstructor
    static class CancelRequestDto {
        private String cancelReason;
    }

    // --- 에러 처리 헬퍼 메소드 (리다이렉트용) ---
    private void handleAuthError(HttpServletResponse response, String message) throws IOException {
        // 간단히 실패 페이지로 리다이렉트 (메시지 포함)
        String errorMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
        // 로그인 페이지나 공통 에러 페이지로 보내는 것이 더 좋을 수 있음
        response.sendRedirect("http://localhost:3000/login?error=" + errorMsg);
    }
}