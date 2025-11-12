package com.daepamarket.daepa_market_backend.admin.check;

import com.daepamarket.daepa_market_backend.admin.delivery.AdminDeliveryRepository;
import com.daepamarket.daepa_market_backend.domain.check.CheckEntity;
import com.daepamarket.daepa_market_backend.domain.check.CheckRepository;
import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckService {

    private final CheckRepository checkRepository;
    private final AdminDeliveryRepository deliveryRepository;
    private final UserRepository userRepository; // 판매자 조회를 위해 UserRepository 추가

    public List<CheckDTO> getAllChecks() {
        return checkRepository.findAllCheckRows().stream()
                .map(row -> {
                    Long ckIdx = ((Number) row[0]).longValue();
                    Long dIdx = ((Number) row[1]).longValue();
                    String productName = (String) row[2];
                    String sellerName = (String) row[3];
                    String tradeType = (String) row[4];
                    Integer ckStatus = ((Number) row[5]).intValue();
                    Integer ckResult = row[6] != null ? ((Number) row[6]).intValue() : null;
                    
                    // 배송 상태 조회
                    Integer dvStatus = null;
                    DeliveryEntity delivery = deliveryRepository.findByCheckCkIdx(ckIdx).orElse(null);
                    if (delivery != null) {
                        dvStatus = delivery.getDvStatus();
                    }
                    
                    return new CheckDTO(ckIdx, dIdx, productName, sellerName, tradeType, ckStatus, ckResult, dvStatus);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateCheckResult(Long ckIdx, int result) {
        CheckEntity check = checkRepository.findById(ckIdx)
                .orElseThrow(() -> new RuntimeException("검수 정보를 찾을 수 없습니다."));
        
        // 이미 검수 완료된 경우 예외 처리
        if (check.getCkStatus() == 1 && check.getCkResult() != null) {
            throw new RuntimeException("이미 검수 결과가 등록된 항목입니다.");
        }
        
        check.setCkStatus(1);
        check.setCkResult(result);
        checkRepository.save(check);
        
        // 배송 정보가 있는 경우에만 배송 상태 업데이트
        DeliveryEntity delivery = deliveryRepository.findByCheckCkIdx(ckIdx).orElse(null);
        if (delivery != null) {
            DealEntity deal = delivery.getDeal();
            UserEntity seller = deal.getSeller();
            double currentManner = seller.getUManner();

            // 검수 결과가 합격(1)이면 배송 시작 (3 : 검수 후 배송)
            if (result == 1) {
                delivery.setDvStatus(3); // 검수 후 배송
                deliveryRepository.save(delivery);

                double newManner = Math.min(100, currentManner + 5.0);
                seller.setUManner(newManner);
                userRepository.save(seller);
            } else if (result == 0) {
                // 불합격이면 반품 처리 (반품: 4)
                delivery.setDvStatus(4); // 반품
                deliveryRepository.save(delivery);

                // === 판매자 매너 온도 차감 로직 추가 ===
                // 매너 온도가 0 이하로 내려가지 않도록 방어 로직 추가 (선택 사항)
                double newManner = Math.max(0, currentManner - 5.0);
                seller.setUManner(newManner);
                userRepository.save(seller);
            }
        }
    }
}