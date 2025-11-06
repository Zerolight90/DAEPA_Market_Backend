package com.daepamarket.daepa_market_backend.deal;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DealService {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final DealRepository dealRepository;

    public ResponseEntity<?> getMySafeDeal(HttpServletRequest request) {

            // 1) 토큰 검증
            String auth = request.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 없습니다.");
            }

            String token = auth.substring(7);
            if (jwtProvider.isExpired(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 만료되었습니다.");
            }

            Long uIdx = Long.valueOf(jwtProvider.getUid(token));
            UserEntity me = userRepository.findById(uIdx)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

            // 2) 내 판매건 + 상품까지 로딩
            List<DealEntity> deals = dealRepository.findWithProductBySeller(me);

            // 3) null 허용해서 직접 Map 만들기
            List<Map<String, Object>> result = deals.stream()
                    .map(d -> {
                        ProductEntity p = d.getProduct(); // fetch join 했으니 보통은 있음
                        Map<String, Object> m = new HashMap<>();
                        m.put("pdTitle", p != null ? p.getPdTitle() : "");
                        m.put("dEdate", d.getDEdate());
                        m.put("agreedPrice", d.getAgreedPrice());
                        return m;
                    })
                    .toList();

            return ResponseEntity.ok(result);
    }
}