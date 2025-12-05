package com.daepamarket.daepa_market_backend.delivery;

import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryDTO;
import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryEntity;
import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class deliveryService {

    private final JwtProvider jwtProvider;
    private final DeliveryRepository deliveryRepository;

    // [보낸 배송]
    public ResponseEntity<?> getMySentDeliveries(HttpServletRequest request) {
        try {
            String token = resolveAccessToken(request);
            if (token == null) return ResponseEntity.status(401).body("토큰이 없습니다.");
            if (jwtProvider.isExpired(token)) return ResponseEntity.status(401).body("유효하지 않은 액세스 토큰입니다.");

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            List<DeliveryDTO> deliveries = deliveryRepository.findSentParcelsBySeller(uIdx);

            return ResponseEntity.ok(deliveries);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    // [받는 배송]
    public ResponseEntity<?> getMyReceivedDeliveries(HttpServletRequest request) {
        try {
            String token = resolveAccessToken(request);
            if (token == null) return ResponseEntity.status(401).body("토큰이 없습니다.");
            if (jwtProvider.isExpired(token)) return ResponseEntity.status(401).body("유효하지 않은 액세스 토큰입니다.");

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            List<DeliveryDTO> list = deliveryRepository.findReceivedParcelsByBuyer(uIdx);
            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    /**
     * Authorization 헤더 또는 ACCESS_TOKEN/ accessToken 쿠키에서 토큰 추출
     */
    private String resolveAccessToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
            return auth.substring(7).trim();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> "ACCESS_TOKEN".equalsIgnoreCase(c.getName()) || "accessToken".equalsIgnoreCase(c.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    // 구매 이력 배송 보냄 확인 버튼
    @Transactional
    public void markAsSent(Long dealId) {
        DeliveryEntity delivery = deliveryRepository.findByDealId(dealId)
                .orElseThrow(() -> new IllegalArgumentException("해당 거래의 배송 정보를 찾을 수 없습니다. dealId=" + dealId));

        // dv_status 업데이트
        delivery.updateStatus(1);
    }

    @Transactional
    public void updateStatus(Long dealId) {
        DeliveryEntity delivery = deliveryRepository.findByDealId(dealId)
                .orElseThrow(() -> new IllegalArgumentException("해당 거래의 배송 정보를 찾을 수 없습니다. dealId=" + dealId));

        // dv_status 업데이트
        delivery.updateStatus(5);
    }
}
