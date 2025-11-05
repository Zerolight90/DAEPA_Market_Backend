package com.daepamarket.daepa_market_backend.pay;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.domain.chat.ChatRoomEntity;
import com.daepamarket.daepa_market_backend.domain.chat.repository.ChatRoomRepository; // ✅ 추가
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.pay.PayEntity;
import com.daepamarket.daepa_market_backend.domain.pay.PayRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.product.ProductRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayService {

    private final RestTemplate restTemplate;
    private final PayRepository payRepository;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final ProductRepository productRepository;
    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;

    // 대파 페이 충전하기
    @Transactional
    public void confirmPointCharge(String paymentKey, String orderId, Long amount, Long userId) {
        confirmToTossPayments(paymentKey, orderId, amount);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다: " + userId));

        Long panprice = payRepository.calculateTotalBalanceByUserId(userId);
        if (panprice == null){
            panprice = 0L;
        }

        // [JPA] pay 테이블에 충전 기록을 생성하고 인서트 하기
        PayEntity chargeLog = new PayEntity();
        chargeLog.setPaDate(LocalDate.now());
        chargeLog.setPaPrice(amount);
        chargeLog.setPaNprice(panprice + amount);
        chargeLog.setPaPoint(0);
        chargeLog.setUser(user);

        payRepository.save(chargeLog);
    }

    @Transactional
    public long getCurrentBalance(Long userId) {
        Long balance = payRepository.calculateTotalBalanceByUserId(userId);
        return balance != null ? balance : 0L;
    }

    // ✅✅ 페이(내 지갑)로 결제시: 결제 성공과 동시에 💸 SYSTEM 메시지 발송
    @Transactional
    public long processPurchaseWithPoints(Long buyerId, Long itemId, int qty, Long amountFromClient) {
        // 구매자 정보 받아오기
        UserEntity buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerId));

        // 상품 정보 가져오기 및 가격 검증
        ProductEntity product = productRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + itemId));

        long correctTotal = product.getPdPrice() * qty;
        if (!amountFromClient.equals(correctTotal)) {
            throw new IllegalArgumentException("요청된 결제 금액이 실제 상품 가격과 일치하지 않습니다.");
        }

        // Deal 엔티티를 비관적 락으로 조회
        // 이 시점부터 다른 트랜잭션이 이 Deal 레코드를 수정할 수 없음
        DealEntity deal = dealRepository.findWithWriteLockByProduct_PdIdx(product.getPdIdx())
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + itemId));

        // Deal 상태 검사
        // d_status가 0L이 아니거나, d_sell이 "판매완료"인 경우
        if (deal.getDStatus() != 0L || "판매완료".equals(deal.getDSell())) {
            throw new IllegalStateException("이미 판매가 완료되었거나 거래가 불가능한 상품입니다.");
        }

        // 현재 대파 페이 잔액 확인 (DB에서 다시 확인 - 동시성 문제 방지)
        long currentBalance = getCurrentBalance(buyerId);
        if (currentBalance < correctTotal) {
            throw new IllegalArgumentException("페이 잔액이 부족합니다.");
        }

        Long panprice = payRepository.calculateTotalBalanceByUserId(buyerId);

        // Pay 테이블에 사용 내역 기록
        PayEntity purchaseLog = new PayEntity();
        purchaseLog.setUser(buyer);
        purchaseLog.setPaPrice(-correctTotal);
        purchaseLog.setPaNprice(panprice - correctTotal);
        purchaseLog.setPaDate(LocalDate.now());
        payRepository.save(purchaseLog);

        // Deal 테이블 업데이트
        deal = dealRepository.findByProduct_PdIdx(product.getPdIdx())
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + itemId));
        deal.setAgreedPrice(correctTotal);
        deal.setBuyer(buyer);
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now()));
        deal.setDBuy("구매확정대기");
        deal.setDSell("판매완료");
        deal.setDStatus(1L);
        dealRepository.save(deal);

        // ✅ 여기서 채팅방 식별 후, 💸 시스템 메시지 발송
        Long roomId = resolveRoomIdByDealOrProduct(deal.getDIdx(), product.getPdIdx());
        if (roomId != null) {
            chatService.sendBuyerDeposited(roomId, buyerId, product.getPdTitle(), deal.getAgreedPrice());
        }

        return currentBalance - correctTotal;
    }

    // ✅✅ 일반(외부 PG) 결제시: 결제 승인 직후 💸 SYSTEM 메시지 발송
    @Transactional
    public void confirmProductPurchase(String paymentKey, String orderId, Long amount){

        // 토스페이먼츠 최종 결제 승인 요청
        confirmToTossPayments(paymentKey, orderId, amount);

        // 주문 정보에서 상품 ID(pdIdx)와 구매자 ID(buyerIdx) 추출
        long pdIdx = extractProductIdFromOrderId(orderId);
        long buyerIdx = extractBuyerIdFromContextOrOrderId(orderId);

        UserEntity buyer = userRepository.findById(buyerIdx)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerIdx));
        DealEntity deal = dealRepository.findByProduct_PdIdx(pdIdx)
                .orElseThrow(() -> new RuntimeException("해당 상품의 거래 정보를 찾을 수 없습니다: " + pdIdx));

        deal.setAgreedPrice(amount);
        deal.setBuyer(buyer);
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now()));
        deal.setDBuy("구매확정 대기");
        deal.setDSell("판매완료");
        deal.setDStatus(0L);
        deal.setPaymentKey(paymentKey);
        deal.setOrderId(orderId);
        dealRepository.save(deal);

        // ✅ 채팅방 식별 후, 💸 시스템 메시지 발송
        ProductEntity product = productRepository.findById(pdIdx)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + pdIdx));
        Long roomId = resolveRoomIdByDealOrProduct(deal.getDIdx(), pdIdx);
        if (roomId != null) {
            chatService.sendBuyerDeposited(roomId, buyerIdx, product.getPdTitle(), amount);
        }
    }

    @Transactional
    public void confirmProductSecPurchase(String paymentKey, String orderId, Long amount){

        // 토스페이먼츠 최종 결제 승인 요청
        confirmToTossPayments(paymentKey, orderId, amount);

        // 주문 정보에서 상품 ID(pdIdx)와 구매자 ID(buyerIdx) 추출
        long pdIdx = extractProductIdFromOrderId(orderId);
        long buyerIdx = extractBuyerIdFromContextOrOrderId(orderId); // 실제 구매자 ID 가져오는 로직 필요

        // 필요한 엔티티 조회
        UserEntity buyer = userRepository.findById(buyerIdx)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerIdx));
        DealEntity deal = dealRepository.findByProduct_PdIdx(pdIdx)
                .orElseThrow(() -> new RuntimeException("해당 상품의 거래 정보를 찾을 수 없습니다: " + pdIdx));

        // Deal 테이블 업데이트
        deal.setAgreedPrice(amount); // 거래 가격
        deal.setBuyer(buyer); // 거래 구매자
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now())); // 거래 시각
        deal.setDBuy("구매확정 대기"); // 구매 상태 (예: 구매 확정 대기)
        deal.setDSell("정산대기");    // 판매 상태
        deal.setDStatus(0L);         // 거래 상태 (예: 1 = 결제완료)
        deal.setPaymentKey(paymentKey);
        deal.setOrderId(orderId);
        dealRepository.save(deal);

        // ✅ 채팅방 식별 후, 💸 시스템 메시지 발송
        ProductEntity product = productRepository.findById(pdIdx)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + pdIdx));
        Long roomId = resolveRoomIdByDealOrProduct(deal.getDIdx(), pdIdx);
        if (roomId != null) {
            chatService.sendBuyerDeposited(roomId, buyerIdx, product.getPdTitle(), amount);
        }
    }

    // ✅ 판매자 “판매 확정” 시 시스템 메시지(📦) — 호출부에서 사용
    @Transactional
    public void confirmSellAndNotify(Long dealId, Long sellerId) {
        DealEntity deal = dealRepository.findById(dealId)
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + dealId));

        // (여기서 d_sell, d_status 등 실제 확정 반영은 기존 서비스 규칙대로)
        // 예: deal.setDSell("판매확정"); dealRepository.save(deal);

        // 채팅방/상품 정보
        Long pdIdx = deal.getProduct().getPdIdx();
        ProductEntity product = productRepository.findById(pdIdx)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + pdIdx));
        Long roomId = resolveRoomIdByDealOrProduct(deal.getDIdx(), pdIdx);

        if (roomId != null) {
            Long price = (deal.getAgreedPrice() != null ? deal.getAgreedPrice() : product.getPdPrice());
            chatService.sendSellerConfirmed(roomId, sellerId, product.getPdTitle(), price);
        }
    }

    // -------------------------------------------- 헬퍼 ----------------------------------------------- //

    // ✅ dealId 우선으로 roomId를 찾고, 없으면 상품 기준 최신 채팅방으로 fallback
    private Long resolveRoomIdByDealOrProduct(Long dealId, Long productId) {
        if (dealId != null) {
            Optional<ChatRoomEntity> byDeal = chatRoomRepository.findByDealId(dealId);
            if (byDeal.isPresent()) return byDeal.get().getChIdx();
        }
        if (productId != null) {
            Optional<ChatRoomEntity> byProduct = chatRoomRepository.findLatestByProductId(productId);
            if (byProduct.isPresent()) return byProduct.get().getChIdx();
        }
        return null;
        // roomId가 null일 수 있는 과거 데이터 케이스 → 메시지는 생략(안전)
    }

    private Long extractUserIdFromChargeOrderId(String orderId) {
        try {
            String[] parts = orderId.split("-");
            if (parts.length > 1 && "charge".equals(parts[0])) {
                return Long.parseLong(parts[1]);
            }
        } catch (Exception e) { /* ignore */ }
        return 2L;
    }

    // 예시: 구매자 ID 추출 (실제 구현 필요)
    private Long extractBuyerIdFromContextOrOrderId(String orderId) {
        return 2L; // TODO 실제 구현
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
    // -------------------------------------------- 헬퍼 메소드 ----------------------------------------------- //
}
