package com.daepamarket.daepa_market_backend.pay;

import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomReq;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomRes;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.chat.service.RoomService; // RoomService 추가
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PayService {

    private final RestTemplate restTemplate;
    private final PayRepository payRepository;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;
    private final ProductRepository productRepository;
    private final ChatService chatService;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomService roomService; // RoomService 추가

    @Value("${TOSS_SECRET_KEY}")
    private String tossSecretKey;

    public PayService(RestTemplate restTemplate, PayRepository payRepository, UserRepository userRepository, DealRepository dealRepository, ProductRepository productRepository, @Lazy ChatService chatService, ChatRoomRepository chatRoomRepository, RoomService roomService) {
        this.restTemplate = restTemplate;
        this.payRepository = payRepository;
        this.userRepository = userRepository;
        this.dealRepository = dealRepository;
        this.productRepository = productRepository;
        this.chatService = chatService;
        this.chatRoomRepository = chatRoomRepository;
        this.roomService = roomService;
    }

    // 대파 페이 충전하기
    @Transactional // 이 메서드 내의 모든 DB 작업을 하나의 트랜잭션으로 묶음
    public void confirmPointCharge(String paymentKey, String orderId, Long amount, Long userId) {

        // 토스페이먼츠에 최종 결제 승인을 요청 (보안상 zustand 등 사용해서 검증하는것 권장됨)
        confirmToTossPayments(paymentKey, orderId, amount);

        // 주문 ID로부터 실제 충전을 요청한 사용자 ID를 가져오기
        // 임시로 1L 유저라고 가정하지만 실제로는 orderId를 DB에 저장하고 매칭하는 과정 (zustand 등)이 권장됨
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 유저를 찾을 수 없습니다: " + userId));

        // 해당하는 유저의 현재 대파 페이 잔액을 얻어내고 만약 null일 경우 0을 넣어주기
        Long panprice = payRepository.calculateTotalBalanceByUserId(userId);
        if (panprice == null){
            panprice = 0L;
        }

        // [JPA] pay 테이블에 충전 기록을 생성하고 인서트 하기
        PayEntity chargeLog = new PayEntity();
        chargeLog.setPaDate(LocalDate.now()); // 충전 시각
        chargeLog.setPaPrice(amount); // 충전 금액
        chargeLog.setPaNprice(panprice + amount); // 현재 금액
        chargeLog.setPaPoint(0); // 포인트는 없음
        chargeLog.setUser(user); // 충전한 유저

        payRepository.save(chargeLog);
        // 만약 여기서 에러가 발생하면 @Transactional을 통해 위에서 변경된 user의 잔액도 자동으로 롤백됨
    }

    // 대파 페이 잔액 조회
    @Transactional
    public long getCurrentBalance(Long userId) {
        // Pay 테이블에서 해당 유저의 모든 거래 내역 합산
        // (PayRepository에 잔액 계산 쿼리 메소드 필요 - 예: findTotalBalanceByUserId)
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

        UserEntity seller = product.getSeller();
        Long sellerId = seller.getUIdx();

        long correctTotal = product.getPdPrice() * qty;
        if (!amountFromClient.equals(correctTotal)) {
            throw new IllegalArgumentException("요청된 결제 금액이 실제 상품 가격과 일치하지 않습니다.");
        }

        // Deal 엔티티를 비관적 락으로 조회
        // 이 시점부터 다른 트랜잭션이 이 Deal 레코드를 수정할 수 없음
        DealEntity deal = dealRepository.findWithWriteLockByProduct_PdIdx(product.getPdIdx())
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + itemId));

        // Deal 상태 검사
        // d_sell이 0L(판매중)이 아닌 경우, 이미 판매되었거나 거래가 불가능한 상품으로 간주
        if (deal.getDSell() != 0L) {
            throw new IllegalStateException("이미 판매가 완료되었거나 거래가 불가능한 상품입니다.");
        }

        // 현재 대파 페이 잔액 확인 (DB에서 다시 확인 - 동시성 문제 방지)
        long currentBalance = getCurrentBalance(buyerId);
        if (currentBalance < correctTotal) {
            throw new IllegalArgumentException("페이 잔액이 부족합니다.");
        }

        Long panprice = payRepository.calculateTotalBalanceByUserId(buyerId);
        Long sellerPanprice = payRepository.calculateTotalBalanceByUserId(sellerId);

        // Pay 테이블에 사용 내역 기록
        PayEntity purchaseLog = new PayEntity();
        purchaseLog.setUser(buyer); // 구매자 유저 설정
        purchaseLog.setPaPrice(-correctTotal); // 사용 금액이므로 음수로 기록
        purchaseLog.setPaNprice(panprice - correctTotal); // 현재 잔액 계산해 설정하기
        purchaseLog.setPaDate(LocalDate.now()); // 결제 날짜 저장
        payRepository.save(purchaseLog);

        // Pay 테이블에 판매자 내역도 기록
        PayEntity sellerLog = new PayEntity();
        sellerLog.setUser(seller);
        sellerLog.setPaPrice(correctTotal);
        sellerLog.setPaNprice(sellerPanprice + correctTotal); // 현재 잔액 계산해 설정하기
        sellerLog.setPaDate(LocalDate.now()); // 결제 날짜 저장
        payRepository.save(sellerLog);

        // Deal 테이블 업데이트
        deal = dealRepository.findByProduct_PdIdx(product.getPdIdx())
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + itemId));
        deal.setAgreedPrice(correctTotal); // 실제 거래된 가격
        deal.setBuyer(buyer); // 구매자 설정
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now())); // 거래 시각 설정
        deal.setDBuy(0L); // 페이 구매 상태
        deal.setDSell(2L); // 페이 판매 상태
        deal.setDStatus(0L); // 결제 상태
        dealRepository.save(deal);

        // ✅ 채팅방 식별 및 생성/조회 후, 💸 시스템 메시지 발송
        // 채팅방이 없을 경우 생성하고, 있을 경우 조회하여 roomId를 확보
        OpenChatRoomReq openChatRoomReq = OpenChatRoomReq.builder()
                .productId(product.getPdIdx())
                .sellerId(sellerId)
                .build();
        OpenChatRoomRes openChatRoomRes = roomService.openOrGetRoom(openChatRoomReq, buyerId);
        Long roomId = openChatRoomRes.getRoomId();

        if (roomId != null) {

            chatService.sendBuyerDeposited(roomId, buyerId, product.getPdTitle(), deal.getAgreedPrice());

            //구매자 명의의 채팅 알림 로직
            try {
                String buyerName = buyer.getUnickname();
                String formattedPrice = NumberFormat.getInstance(Locale.KOREA).format(deal.getAgreedPrice());
                String message = String.format("💸 결제 완료 알림\n\n%s님이 %s원을 입금했어요.\n상품 상태를 [판매 완료]로 변경해주세요!", buyerName, formattedPrice);
                chatService.sendMessage(roomId, buyerId, message, null, null);
            } catch (Exception e) {
                log.error("구매자 명의 입금 채팅 알림 전송 중 오류 발생", e);
            }
            //
        }

        return currentBalance - correctTotal;
    }

    // ✅✅ 일반(외부 PG) 결제시: 결제 승인 직후 💸 SYSTEM 메시지 발송
    @Transactional
    public void confirmProductPurchase(String paymentKey, String orderId, Long amount, Long buyerIdx){

        // 토스페이먼츠 최종 결제 승인 요청
        confirmToTossPayments(paymentKey, orderId, amount);

        // 주문 정보에서 상품 ID(pdIdx)와 구매자 ID(buyerIdx) 추출
        long pdIdx = extractProductIdFromOrderId(orderId);

        // 필요한 엔티티 조회
        UserEntity buyer = userRepository.findById(buyerIdx)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerIdx));
        DealEntity deal = dealRepository.findWithWriteLockByProduct_PdIdx(pdIdx)
                .orElseThrow(() -> new RuntimeException("해당 상품의 거래 정보를 찾을 수 없습니다: " + pdIdx));

        // Deal 상태 검사
        // d_sell이 0L(판매중)이 아닌 경우, 이미 판매되었거나 거래가 불가능한 상품으로 간주
        if (deal.getDSell() != 0L) {
            throw new IllegalStateException("이미 판매가 완료되었거나 거래가 불가능한 상품입니다.");
        }

        // Deal 테이블 업데이트
        deal.setAgreedPrice(amount); // 거래 가격
        deal.setBuyer(buyer); // 거래 구매자
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now())); // 거래 시각
        deal.setDBuy(0L); // 구매 상태 (예: 구매 확정 대기)
        deal.setDSell(2L);    // 판매 상태
        deal.setDStatus(0L);         // 거래 상태 (예: 1 = 결제완료)
        deal.setPaymentKey(paymentKey);
        deal.setOrderId(orderId);
        dealRepository.save(deal);

        // ✅ 채팅방 식별 및 생성/조회 후, 💸 시스템 메시지 발송
        // 채팅방이 없을 경우 생성하고, 있을 경우 조회하여 roomId를 확보
        ProductEntity product = productRepository.findById(pdIdx)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + pdIdx));
        UserEntity seller = product.getSeller();
        Long sellerId = seller.getUIdx();

        OpenChatRoomReq openChatRoomReq = OpenChatRoomReq.builder()
                .productId(pdIdx)
                .sellerId(sellerId)
                .build();
        OpenChatRoomRes openChatRoomRes = roomService.openOrGetRoom(openChatRoomReq, buyerIdx);
        Long roomId = openChatRoomRes.getRoomId();

        if (roomId != null) {
            chatService.sendBuyerDeposited(roomId, buyerIdx, product.getPdTitle(), amount);

            // 매자 명의의 채팅 알림 로직
            try {
                String buyerName = buyer.getUnickname();
                String formattedPrice = NumberFormat.getInstance(Locale.KOREA).format(amount);
                String message = String.format("💸 결제 완료 알림\n\n%s님이 %s원을 입금했어요.\n상품 상태를 [판매 완료]로 변경해주세요!", buyerName, formattedPrice);                chatService.sendMessage(roomId, buyerIdx, message, null, null);
            } catch (Exception e) {
                log.error("구매자 명의 입금 채팅 알림 전송 중 오류 발생", e);
            }
            //
        }
    }

    // 대파페이 안전결제
    @Transactional
    public long processSecPurchaseWithPoints(Long buyerId, Long itemId, int qty, Long amountFromClient) {
        // 구매자 정보 받아오기
        UserEntity buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerId));

        // 상품 정보 가져오기 및 가격 검증
        ProductEntity product = productRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + itemId));

        UserEntity seller = product.getSeller();
        Long sellerId = seller.getUIdx();

        long correctTotal = product.getPdPrice() * qty;
        if (!amountFromClient.equals(correctTotal)) {
            throw new IllegalArgumentException("요청된 결제 금액이 실제 상품 가격과 일치하지 않습니다.");
        }

        // Deal 엔티티를 비관적 락으로 조회
        // 이 시점부터 다른 트랜잭션이 이 Deal 레코드를 수정할 수 없음
        DealEntity deal = dealRepository.findWithWriteLockByProduct_PdIdx(product.getPdIdx())
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + itemId));

        // Deal 상태 검사
        // d_sell이 0L(판매중)이 아닌 경우, 이미 판매되었거나 거래가 불가능한 상품으로 간주
        if (deal.getDSell() != 0L) {
            throw new IllegalStateException("이미 판매가 완료되었거나 거래가 불가능한 상품입니다.");
        }

        // 현재 대파 페이 잔액 확인 (DB에서 다시 확인 - 동시성 문제 방지)
        long currentBalance = getCurrentBalance(buyerId);
        if (currentBalance < correctTotal) {
            throw new IllegalArgumentException("페이 잔액이 부족합니다.");
        }

        // 일반결제와 다르게 구매자가 구매 확정을 누르는 부분에서 해당 내용이 수행되어야 함
//        Long panprice = payRepository.calculateTotalBalanceByUserId(buyerId);
//        Long sellerPanprice = payRepository.calculateTotalBalanceByUserId(sellerId);
//
//        // Pay 테이블에 사용 내역 기록
//        PayEntity purchaseLog = new PayEntity();
//        purchaseLog.setUser(buyer); // 구매자 유저 설정
//        purchaseLog.setPaPrice(-correctTotal); // 사용 금액이므로 음수로 기록
//        purchaseLog.setPaNprice(panprice - correctTotal); // 현재 잔액 계산해 설정하기
//        purchaseLog.setPaDate(LocalDate.now()); // 결제 날짜 저장
//        payRepository.save(purchaseLog);
//
//        // Pay 테이블에 판매자 내역도 기록
//        PayEntity sellerLog = new PayEntity();
//        sellerLog.setUser(seller);
//        sellerLog.setPaPrice(correctTotal);
//        sellerLog.setPaNprice(sellerPanprice + correctTotal); // 현재 잔액 계산해 설정하기
//        sellerLog.setPaDate(LocalDate.now()); // 결제 날짜 저장
//        payRepository.save(sellerLog);

        // Deal 테이블 업데이트
        deal = dealRepository.findByProduct_PdIdx(product.getPdIdx())
                .orElseThrow(() -> new RuntimeException("거래 정보를 찾을 수 없습니다: " + itemId));
        deal.setAgreedPrice(correctTotal); // 실제 거래된 가격
        deal.setBuyer(buyer); // 구매자 설정
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now())); // 거래 시각 설정
        deal.setDBuy(0L); // 페이 구매 상태
        deal.setDSell(2L); // 페이 판매 상태
        deal.setDStatus(0L); // 결제 상태
        dealRepository.save(deal);

        // ✅ 채팅방 식별 및 생성/조회 후, 💸 시스템 메시지 발송
        // 채팅방이 없을 경우 생성하고, 있을 경우 조회하여 roomId를 확보
        OpenChatRoomReq openChatRoomReq = OpenChatRoomReq.builder()
                .productId(product.getPdIdx())
                .sellerId(sellerId)
                .build();
        OpenChatRoomRes openChatRoomRes = roomService.openOrGetRoom(openChatRoomReq, buyerId);
        Long roomId = openChatRoomRes.getRoomId();

        if (roomId != null) {
            chatService.sendBuyerDeposited(roomId, buyerId, product.getPdTitle(), deal.getAgreedPrice());
        }

        return currentBalance - correctTotal;
    }

    @Transactional
    public void confirmProductSecPurchase(String paymentKey, String orderId, Long amount, Long buyerIdx){

        // 토스페이먼츠 최종 결제 승인 요청
        confirmToTossPayments(paymentKey, orderId, amount);

        // 주문 정보에서 상품 ID(pdIdx)와 구매자 ID(buyerIdx) 추출
        long pdIdx = extractProductIdFromOrderId(orderId);

        // 필요한 엔티티 조회
        UserEntity buyer = userRepository.findById(buyerIdx)
                .orElseThrow(() -> new RuntimeException("구매자 정보를 찾을 수 없습니다: " + buyerIdx));
        DealEntity deal = dealRepository.findWithWriteLockByProduct_PdIdx(pdIdx)
                .orElseThrow(() -> new RuntimeException("해당 상품의 거래 정보를 찾을 수 없습니다: " + pdIdx));

        // Deal 상태 검사
        // d_sell이 0L(판매중)이 아닌 경우, 이미 판매되었거나 거래가 불가능한 상품으로 간주
        if (deal.getDSell() != 0L) {
            throw new IllegalStateException("이미 판매가 완료되었거나 거래가 불가능한 상품입니다.");
        }

        // Deal 테이블 업데이트
        deal.setAgreedPrice(amount); // 거래 가격
        deal.setBuyer(buyer); // 거래 구매자
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now())); // 거래 시각
        deal.setDBuy(0L);     // 구매 상태 (0 = 미구매, 1 = 구매 확정)
        deal.setDSell(2L);    // 판매 상태 (0 = 판매중, 1 = 판매완료, 2 = 입금완료)
        deal.setDStatus(0L);  // 거래 상태 (0 = 거래중, 1 = 거래완료)
        deal.setPaymentKey(paymentKey);
        deal.setOrderId(orderId);
        dealRepository.save(deal);

        // ✅ 채팅방 식별 및 생성/조회 후, 💸 시스템 메시지 발송
        // 채팅방이 없을 경우 생성하고, 있을 경우 조회하여 roomId를 확보
        ProductEntity product = productRepository.findById(pdIdx)
                .orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다: " + pdIdx));
        UserEntity seller = product.getSeller();
        Long sellerId = seller.getUIdx();

        OpenChatRoomReq openChatRoomReq = OpenChatRoomReq.builder()
                .productId(pdIdx)
                .sellerId(sellerId)
                .build();
        OpenChatRoomRes openChatRoomRes = roomService.openOrGetRoom(openChatRoomReq, buyerIdx);
        Long roomId = openChatRoomRes.getRoomId();

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

    /**
     * ✅ [신규] 구매 확정 처리 (안전결제)
     * @param dealId 확정할 거래 ID
     * @param buyerId 확정을 요청한 사용자 ID (구매자)
     */
    @Transactional
    public void finalizePurchase(Long dealId, Long buyerId) {
        // 1. 거래 정보 조회 (비관적 락으로 동시성 제어)
        DealEntity deal = dealRepository.findWithWriteLockByDIdx(dealId)
                .orElseThrow(() -> new IllegalStateException("거래 정보를 찾을 수 없습니다."));

        // 2. 권한 검증: 요청한 사용자가 실제 구매자인지 확인
        if (deal.getBuyer() == null || !deal.getBuyer().getUIdx().equals(buyerId)) {
            throw new AccessDeniedException("이 거래를 확정할 권한이 없습니다.");
        }

        // 3. 상태 검증: '판매중' 상태가 맞는지 확인
        if (deal.getDStatus() != 0L) {
            throw new IllegalStateException("이미 처리되었거나 구매 확정 대기 상태가 아닌 거래입니다.");
        }

        // 4. 거래 상태 '거래 완료'로 변경
        deal.setDBuy(1L);
        deal.setDStatus(1L); // 1L = 거래 완료
        deal.setDEdate(Timestamp.valueOf(LocalDateTime.now())); // 거래 완료 시각 기록

        // 5. 판매자에게 정산 처리
        UserEntity seller = deal.getSeller();
        Long sellerId = seller.getUIdx();
        Long price = deal.getAgreedPrice();

        // 판매자의 현재 페이 잔액 조회
        Long sellerBalance = payRepository.calculateTotalBalanceByUserId(sellerId);
        if (sellerBalance == null) {
            sellerBalance = 0L;
        }

        // Pay 테이블에 판매자 입금 내역 기록
        PayEntity sellerLog = new PayEntity();
        sellerLog.setUser(seller);
        sellerLog.setPaPrice(price); // 판매 금액 (양수)
        sellerLog.setPaNprice(sellerBalance + price); // 새로운 잔액
        sellerLog.setPaDate(LocalDate.now());
        payRepository.save(sellerLog);

        // @Transactional에 의해 deal과 sellerLog는 자동으로 저장/커밋됨
    }

    @Transactional
    public void cancelProductPurchase(Long dealId, Long currentUserId, String cancelReason) throws AccessDeniedException {

        // 1. 거래(Deal) 정보 조회 (비관적 락 추천)
        DealEntity deal = dealRepository.findWithWriteLockByDIdx(dealId) // (findWithWriteLockByProduct_PdIdx는 DealRepository에 @Lock 추가 필요)
                .orElseThrow(() -> new RuntimeException("환불할 거래 정보를 찾을 수 없습니다: " + dealId));

        // 2. 권한 검증: 현재 로그인한 사용자가 구매자가 맞는지 확인
        if (deal.getSeller() == null || !deal.getSeller().getUIdx().equals(currentUserId)) {
            throw new AccessDeniedException("이 거래를 환불할 권한이 없습니다.");
        }

        // 3. 상태 검증: 이미 취소되었는지 확인
        // (DealEntity의 dBuy, dStatus 컬럼 타입과 취소 상태값 확인 필요)
        if (deal.getDBuy() == 3L || deal.getDStatus() == 2L) { // 2L = 취소 상태 (예시)
            throw new IllegalStateException("이미 환불된 거래입니다.");
        }

        // 4. Deal에 저장된 paymentKey 가져오기
        String paymentKey = deal.getPaymentKey();
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new RuntimeException("결제 정보(paymentKey)가 없어 환불이 불가능합니다.");
        }

        // 5. 토스페이먼츠 환불 API 호출
        callTossCancelApi(paymentKey, (cancelReason != null ? cancelReason : "검수 불합격"));

        // 6. Deal 테이블 상태 업데이트 (취소 상태로 변경)
        deal.setDBuy(0L);
        deal.setDSell(0L); // 또는 판매자가 다시 판매할 수 있도록 "판매중"
        deal.setDStatus(0L); // 2 = 취소 (예시)
        // deal.setDEdate(null); // 거래 완료 시간 초기화 (선택 사항)

        // dealRepository.save(deal); // @Transactional이므로 Dirty Checking에 의해 자동 저장
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

    private void callTossCancelApi(String paymentKey, String cancelReason) {
        String url = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

        // 1. HTTP 헤더 설정 (Basic Auth)
        HttpHeaders headers = new HttpHeaders();
        String encodedKey = Base64.getEncoder().encodeToString((tossSecretKey + ":").getBytes());
        headers.setBasicAuth(encodedKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. HTTP 바디 설정 (전액 취소 시 cancelAmount 불필요)
        Map<String, String> bodyMap = Map.of("cancelReason", cancelReason);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(bodyMap, headers);

        try {
            // 3. API 호출
            restTemplate.postForEntity(url, request, String.class);
            // 성공 시 Toss에서 200 OK와 취소 내역 JSON 반환

        } catch (Exception e) {
            // API 호출 실패 (Toss에서 4xx/5xx 에러 반환)
            System.err.println("Toss Payments 환불 API 호출 실패: " + e.getMessage());
            // TODO: Toss API 에러 메시지를 파싱하여 사용자에게 더 친절한 메시지 반환
            throw new RuntimeException("결제 취소(환불)에 실패했습니다. (API 오류)");
        }
    }
}