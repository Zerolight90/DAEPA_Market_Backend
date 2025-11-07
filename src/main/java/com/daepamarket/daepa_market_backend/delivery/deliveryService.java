package com.daepamarket.daepa_market_backend.delivery;

import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryDTO;
import com.daepamarket.daepa_market_backend.domain.delivery.DeliveryRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
public class deliveryService {

    private final JwtProvider jwtProvider;
    private final DeliveryRepository deliveryRepository;

    // [보낸 택배]
    public ResponseEntity<?> getMySentDeliveries(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("토큰이 없습니다.");
            }

            String token = auth.substring(7);

            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));

            List<DeliveryDTO> deliveries = deliveryRepository.findSentParcelsBySeller(uIdx);

            return ResponseEntity.ok(deliveries);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }

    }

    // [받은 택배]
    public ResponseEntity<?> getMyReceivedDeliveries(HttpServletRequest request) {
        try {
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("토큰이 없습니다.");
            }

            String token = auth.substring(7);
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            List<DeliveryDTO> list = deliveryRepository.findReceivedParcelsByBuyer(uIdx);
            return ResponseEntity.ok(list);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }
}