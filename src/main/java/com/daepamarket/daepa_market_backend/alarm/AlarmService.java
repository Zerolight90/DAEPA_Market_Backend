package com.daepamarket.daepa_market_backend.alarm;

import com.daepamarket.daepa_market_backend.domain.alarm.AlarmEntity;
import com.daepamarket.daepa_market_backend.domain.alarm.AlarmRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.userpick.UserPickEntity;
import com.daepamarket.daepa_market_backend.domain.userpick.UserPickRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final UserPickRepository userPickRepository;
    private final AlarmRepository alarmRepository;

    // @Async // 알림 생성이 오래 걸릴 경우 비동기 처리 고려
    @Transactional // 별도의 트랜잭션으로 관리하거나, ProductService 트랜잭션에 참여
    public void createAlarmsForMatchingProduct(ProductEntity product) {
        log.info("상품 매칭 알림 생성 시작: productId={}", product.getPdIdx());

        // 1. 새로 등록된 상품 정보로 매칭되는 UserPick 목록 조회
        List<UserPickEntity> matchingPicks = userPickRepository.findMatchingPicks(
                product.getCtLow(),
                product.getPdPrice()    // ProductEntity의 가격 필드명 확인 필요
        );

        if (matchingPicks.isEmpty()) {
            log.info("매칭되는 관심 상품 없음: productId={}", product.getPdIdx());
            return; // 매칭되는 항목 없으면 종료
        }

        List<AlarmEntity> alarmsToCreate = new ArrayList<>();
        for (UserPickEntity pick : matchingPicks) {
            // (선택) 상품 등록자와 관심 상품 소유자가 같으면 알림 생성 X
            if (pick.getUser().getUIdx().equals(product.getSeller().getUIdx())) {
                continue;
            }

            AlarmEntity alarm = AlarmEntity.builder()
                    .user(pick.getUser()) // 알림 받을 사용자 (UserPick 소유자)
                    .product(product) // 새로 등록된 상품
                    .alType("MATCHING_PRODUCT") // 알림 유형 지정
                    .alRead(false)
                    .alDel(false)
                    .alCreate(LocalDateTime.now())
                    .build();
            alarmsToCreate.add(alarm);
        }

        // 3. 생성된 알림 목록을 DB에 한 번에 저장
        if (!alarmsToCreate.isEmpty()) {
            alarmRepository.saveAll(alarmsToCreate);
            log.info("{}개의 매칭 알림 생성 완료: productId={}", alarmsToCreate.size(), product.getPdIdx());
        }
    }

    @Transactional
    public void deleteNotification(Long uIdx, Long productId) {
        alarmRepository.deleteByUIdxAndProductId(uIdx, productId);
    }
}