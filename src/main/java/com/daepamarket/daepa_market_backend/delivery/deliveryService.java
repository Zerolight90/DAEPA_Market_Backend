//package com.daepamarket.daepa_market_backend.delivery;
//
//import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
//import com.daepamarket.daepa_market_backend.domain.deal.DealRepository;
//import com.daepamarket.daepa_market_backend.domain.delivery.deliveryEntity;
//import com.daepamarket.daepa_market_backend.domain.delivery.deliveryRepository;
//import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
//import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
//import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class deliveryService {
//
//    private final JwtProvider jwtProvider;
//    private final UserRepository userRepository;
//    private final DealRepository dealRepository;
//    private final deliveryRepository deliveryRepository;
//
//    public ResponseEntity<?> getSentList(HttpServletRequest request) {
//
//        // 1) 토큰 꺼내기
//        String auth = request.getHeader("Authorization");
//        if (auth == null || !auth.startsWith("Bearer ")) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 없습니다.");
//        }
//        String token = auth.substring(7);
//        if (jwtProvider.isExpired(token)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 만료되었습니다.");
//        }
//
//        Long myIdx = Long.valueOf(jwtProvider.getUid(token));
//
//        // 2) 유저 찾기
//        UserEntity me = userRepository.findById(myIdx)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
//
//        // 3) 내가 판매자였던 딜들
//        List<DealEntity> myDeals = dealRepository.findBySeller(me);
//
//        // 4) 각 딜에 대해 delivery 중 dv_status = 1 인 것만 뽑아서 응답으로 묶기
//        List<Map<String, Object>> result = new ArrayList<>();
//
//        for (DealEntity deal : myDeals) {
//            List<deliveryEntity> deliveries = deliveryRepository.findByDealAndDvStatus(deal, 1);
//            if (deliveries.isEmpty()) {
//                continue; // 이 딜은 아직 발송 안 됨
//            }
//
//            // 발송 건이 여러 개여도 화면에서는 딜당 하나만 보여도 된다면 첫 번째만 써도 됨
//            for (deliveryEntity dv : deliveries) {
//                Map<String, Object> row = new HashMap<>();
//                row.put("dIdx", deal.getDIdx());
//                row.put("pdTitle",
//                        deal.getProduct() != null ? deal.getProduct().getPdTitle() : "");
//                row.put("agreedPrice", deal.getAgreedPrice());
//                row.put("dEdate", deal.getDEdate());
//                row.put("dvIdx", dv.getDvIdx());
//                row.put("dvStatus", dv.getDvStatus());
//                // 필요하면 배송지, 운송장 등 나중에 더 추가
//                result.add(row);
//            }
//        }
//
//        return ResponseEntity.ok(result);
//    }
//}