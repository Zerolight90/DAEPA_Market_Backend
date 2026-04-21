package com.daepamarket.daepa_market_backend.delivery;

import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryDTO;
import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryEntity;
import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryRepository;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final JwtProvider jwtProvider;
    private final DeliveryRepository deliveryRepository;
    private final CookieUtil cookieUtil;

    public ResponseEntity<?> getMySentDeliveries(HttpServletRequest request) {
        return processDeliveryRequest(request, true);
    }

    public ResponseEntity<?> getMyReceivedDeliveries(HttpServletRequest request) {
        return processDeliveryRequest(request, false);
    }

    private ResponseEntity<?> processDeliveryRequest(HttpServletRequest request, boolean isSent) {
        try {
            String token = cookieUtil.getAccessTokenFromCookie(request);
            if (token == null || token.isBlank()) return ResponseEntity.status(401).body("토큰이 없습니다.");
            if (jwtProvider.isExpired(token)) return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            List<DeliveryDTO> deliveries = isSent
                    ? deliveryRepository.findSentParcelsBySeller(uIdx)
                    : deliveryRepository.findReceivedParcelsByBuyer(uIdx);

            return ResponseEntity.ok(deliveries);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    @Transactional
    public void markAsSent(Long dealId) {
        DeliveryEntity delivery = deliveryRepository.findByDealId(dealId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보 없음. dealId=" + dealId));
        delivery.updateStatus(1);
    }

    @Transactional
    public void updateStatus(Long dealId) {
        DeliveryEntity delivery = deliveryRepository.findByDealId(dealId)
                .orElseThrow(() -> new IllegalArgumentException("배송 정보 없음. dealId=" + dealId));
        delivery.updateStatus(5);
    }
}
