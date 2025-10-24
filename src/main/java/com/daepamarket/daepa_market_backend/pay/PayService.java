package com.daepamarket.daepa_market_backend.pay;

import java.time.LocalDate;

import com.daepamarket.daepa_market_backend.domain.pay.PayEntity;
import com.daepamarket.daepa_market_backend.domain.pay.PayRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;

import jakarta.transaction.Transactional;

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

    @Transactional // 이 메서드 내의 모든 DB 작업을 하나의 트랜잭션으로 묶음
    public void confirmPaymentAndUpdateDb(String paymentKey, String orderId, Long amount) {
        
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

    // 토스페이먼츠 API를 호출하여 결제를 최종 승인하는 메서드
    private void confirmToTossPayments(String paymentKey, String orderId, Long amount) {
        // ... (이전 답변에서 설명한 RestTemplate으로 토스 API 호출하는 로직)
        // 요청 실패 시 Exception을 발생시켜 트랜잭션이 롤백되도록 함
        System.out.println("토스페이먼츠에 결제 승인을 요청합니다.");
    }
}