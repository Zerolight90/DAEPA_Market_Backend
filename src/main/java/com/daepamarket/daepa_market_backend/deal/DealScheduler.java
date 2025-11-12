package com.daepamarket.daepa_market_backend.deal;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.pay.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DealScheduler {

    private final DealRepository dealRepository;
    private final PayService payService;

    /**
     * 매일 새벽 3시에 실행되어, 판매자가 '판매 완료' 처리한 지 15일이 지났지만
     * 구매자가 '구매 확정'을 누르지 않은 거래를 자동으로 처리합니다.
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시에 실행
    @Transactional
    public void applyPenaltyAndAutoCompleteDeals() {
        log.info("15일 이상 미확정 거래 자동 처리 스케줄러 시작...");

        // 1. 15일 전 날짜 계산
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(15);

        // 2. 15일 이상 미확정된 거래 목록 조회
        List<DealEntity> targetDeals = dealRepository.findUnconfirmedDealsOlderThan(cutoffDate);

        if (targetDeals.isEmpty()) {
            log.info("자동 처리할 미확정 거래가 없습니다.");
            return;
        }

        log.info("{}건의 미확정 거래를 자동으로 처리합니다.", targetDeals.size());

        // 3. 각 거래에 대해 패널티 적용 및 자동 확정 처리
        for (DealEntity deal : targetDeals) {
            try {
                UserEntity buyer = deal.getBuyer();
                if (buyer == null) {
                    log.warn("거래 ID {}에 구매자 정보가 없어 처리할 수 없습니다.", deal.getDIdx());
                    continue;
                }

                // 3-1. 구매자 매너 온도 차감 (-5점, 최솟값 0)
                double currentManner = buyer.getUManner();
                double newManner = Math.max(0, currentManner - 5.0);
                buyer.setUManner(newManner);
                // payService.finalizePurchase 내부에서 buyer 정보는 저장되지 않으므로, 여기서 명시적으로 저장할 필요는 없습니다.
                // finalizePurchase가 buyerId만 사용하기 때문입니다. 하지만 명시적으로 분리된 로직을 원한다면 userRepository.save(buyer)가 필요합니다.
                // 현재 구조에서는 finalizePurchase가 모든 것을 처리하도록 위임합니다.

                log.info("거래 ID: {}, 구매자 ID: {} 매너 온도 차감 적용 ({} -> {})", deal.getDIdx(), buyer.getUIdx(), currentManner, newManner);

                // 3-2. 거래 자동 확정 처리 (판매자에게 정산)
                // finalizePurchase는 내부적으로 권한 검증을 하므로, 스케줄러에서 호출 시 buyerId를 정확히 넘겨주는 것이 중요합니다.
                payService.finalizePurchase(deal.getDIdx(), buyer.getUIdx());

                log.info("거래 ID: {}가 자동으로 구매 확정 처리되었습니다.", deal.getDIdx());

            } catch (Exception e) {
                // 개별 거래 처리 중 오류가 발생해도 전체 스케줄러가 멈추지 않도록 처리
                log.error("미확정 거래(ID: {}) 자동 처리 중 오류 발생", deal.getDIdx(), e);
            }
        }

        log.info("15일 이상 미확정 거래 자동 처리 스케줄러 완료.");
    }
}
