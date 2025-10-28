package com.daepamarket.daepa_market_backend.pay;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.pay.PayEntity;
import com.daepamarket.daepa_market_backend.domain.pay.PayRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;

import jakarta.transaction.Transactional;

import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayService {

    // @Value("${toss.secretKey}")
    // private String tossSecretKey;

    private final RestTemplate restTemplate; // API 호출을 위함
    private final PayRepository payRepository; // JPA
    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final ProductRepository productRepository;

    // --- 페이 잔액 조회 ---
    @Transactional
    public long getCurrentBalance(Long userId) {
        // Pay 테이블에서 해당 유저의 모든 거래 내역 합산
        // (PayRepository에 잔액 계산 쿼리 메소드 필요 - 예: findTotalBalanceByUserId)
        Long balance = payRepository.calculateTotalBalanceByUserId(userId);
        return balance != null ? balance : 0L; // null이면 0 반환
    }

    // --- 페이로 상품 구매 처리 ---
    @Transactional // 중요: 여러 DB 업데이트가 있으므로 트랜잭션 필수
    public long processPurchaseWithPoints(Long buyerId, Long itemId, int qty, Long amountFromClient) {

        // 1. 구매자 정보 로드
        UserEntity buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerId));

        // 2. 상품 정보 로드 및 가격 검증 (★중요★)
        ProductEntity product = productRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + itemId));
        long correctTotal = product.getPdPrice() * qty; // DB 가격 * 수량

        if (!amountFromClient.equals(correctTotal)) {
            throw new IllegalArgumentException("요청된 결제 금액이 실제 상품 가격과 일치하지 않습니다.");
        }

        // 3. 현재 잔액 확인 (DB에서 다시 확인 - 동시성 문제 방지)
        long currentBalance = getCurrentBalance(buyerId);
        if (currentBalance < correctTotal) {
            throw new IllegalArgumentException("페이 잔액이 부족합니다.");
        }

        Optional<UserEntity> user = userRepository.findById(buyerId);

        // 4. Pay 테이블에 사용 내역 기록 (★차감★)
        PayEntity purchaseLog = new PayEntity();
        // purchaseLog.setUser(user);
        purchaseLog.setPaPrice(-correctTotal); // ✅ 사용 금액은 음수로 기록
        purchaseLog.setPaDate(LocalDate.now());
        // purchaseLog.setDIdx(...); // 필요 시 Deal ID 연결
        // ... 기타 정보 ...
        payRepository.save(purchaseLog);

        // 5. Deal 테이블 업데이트
        DealEntity deal = dealRepository.findByProduct_PdIdx(product.getPdIdx())
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + itemId));

        deal.setAgreedPrice(correctTotal); // 실제 거래된 가격
        deal.setBuyer(buyer);
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now()));
        deal.setDBuy("구매확정대기"); // 페이 구매 상태
        deal.setDSell("판매완료");
        deal.setDStatus(1L); // 결제 완료 상태

        dealRepository.save(deal);

        // 6. 남은 잔액 계산하여 반환
        return currentBalance - correctTotal;
    }

    @Transactional // 이 메서드 내의 모든 DB 작업을 하나의 트랜잭션으로 묶음
    public void confirmPointCharge(String paymentKey, String orderId, Long amount) {
        
        // 1. 토스페이먼츠에 최종 결제 승인을 요청합니다. (보안상 필수, zustand 사용해서 검증하는것 추가해야함!)
        confirmToTossPayments(paymentKey, orderId, amount);

        // 2. 주문 ID로부터 실제 충전을 요청한 사용자 ID를 가져옵니다.
        // 임시로 1L 유저라고 가정
        // 실제로는 orderId를 DB에 저장하고 매칭하는 과정 (zustand)이 필요함!!
        Long userId = (long)2;

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다: " + userId));

        // 3. [JPA만 사용] 'pay' 테이블에 충전 기록을 생성하고 저장(INSERT)합니다.
        PayEntity chargeLog = new PayEntity();
        chargeLog.setPaDate(LocalDate.now());
        chargeLog.setPaPrice(amount);     // ✅ 충전이므로 양수로 금액 기록
        chargeLog.setPaNprice(amount);    // 실 결제액
        chargeLog.setPaPoint(0);  // 사용 포인트 없음
        chargeLog.setUser(user);
        // chargeLog.setDIdx(null);       // 거래 ID는 충전이므로 없음

        payRepository.save(chargeLog);
        // 만약 여기서 에러가 발생하면? @Transactional 덕분에
        // 위에서 변경된 user의 잔액(u_balance)도 자동으로 롤백(원상복구)됩니다.
    }

    @Transactional
    public void confirmProductPurchase(String paymentKey, String orderId, Long amount){

        // 1. 토스페이먼츠 최종 결제 승인 요청
        confirmToTossPayments(paymentKey, orderId, amount);

        // 2. 주문 정보에서 상품 ID(pdIdx)와 구매자 ID(buyerIdx) 추출
        long pdIdx = extractProductIdFromOrderId(orderId);
        long buyerIdx = extractBuyerIdFromContextOrOrderId(orderId); // 실제 구매자 ID 가져오는 로직 필요

        // 3. 필요한 엔티티 조회
        ProductEntity product = productRepository.findById(pdIdx)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + pdIdx));
        UserEntity buyer = userRepository.findById(buyerIdx)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerIdx));
        DealEntity deal = dealRepository.findByProduct_PdIdx(pdIdx)
                .orElseThrow(() -> new RuntimeException("해당 상품의 거래 정보를 찾을 수 없습니다: " + pdIdx));

        // 4. Deal 테이블 업데이트
        deal.setAgreedPrice(amount);
        deal.setBuyer(buyer);
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now()));
        deal.setDBuy("구매확정 대기"); // 구매 상태 (예: 구매 확정 대기)
        deal.setDSell("판매완료");    // 판매 상태
        deal.setDStatus(0L);         // 거래 상태 (예: 1 = 결제완료)
        deal.setPaymentKey(paymentKey);
        deal.setOrderId(orderId);

        dealRepository.save(deal);

    }

    // 예시: 충전 주문 ID("charge-${userId}-${uuid}")에서 사용자 ID 추출
    private Long extractUserIdFromChargeOrderId(String orderId) {
        try {
            String[] parts = orderId.split("-");
            if (parts.length > 1 && "charge".equals(parts[0])) {
                return Long.parseLong(parts[1]);
            }
        } catch (Exception e) { /* ignore */ }
        // 실제로는 더 안정적인 방법 사용 권장 (예: DB 조회)
        // 임시로 하드코딩된 ID 반환 (테스트용)
        return 2L;
    }

    // 예시: 구매자 ID 추출 (실제 구현 필요)
    private Long extractBuyerIdFromContextOrOrderId(String orderId) {
        // TODO: Spring Security Context Holder에서 현재 로그인 사용자 ID를 가져오거나,
        // orderId 생성 시 구매자 정보를 포함시키는 등 실제 구매자 ID를 가져오는 로직 구현 필요
        return 2L; // 임시 구매자 ID
    }

    // 예시: 상품 구매 주문 ID("product-${pdIdx}-${uuid}")에서 상품 ID 추출
    private Long extractProductIdFromOrderId(String orderId) {
        try {
            String[] parts = orderId.split("-");
            if (parts.length > 1 && "product".equals(parts[0])) {
                return Long.parseLong(parts[1]);
            }
        } catch (Exception e) { /* ignore */ }
        throw new IllegalArgumentException("주문 ID에서 상품 ID를 추출할 수 없습니다: " + orderId);
    }

    // 토스페이먼츠 API를 호출하여 결제를 최종 승인하는 메서드
    private void confirmToTossPayments(String paymentKey, String orderId, Long amount) {
        // ... (이전 답변에서 설명한 RestTemplate으로 토스 API 호출하는 로직)
        // 요청 실패 시 Exception을 발생시켜 트랜잭션이 롤백되도록 함
        System.out.println("토스페이먼츠에 결제 승인을 요청합니다.");
    }
}