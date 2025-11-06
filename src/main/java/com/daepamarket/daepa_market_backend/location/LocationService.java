package com.daepamarket.daepa_market_backend.location;

import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public ResponseEntity<?> addLocation(HttpServletRequest request, Map<String, Object> body) {

        // 1) 토큰 꺼내기
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(UNAUTHORIZED, "토큰이 없습니다.");
        }
        String token = auth.substring(7);
        if (jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(UNAUTHORIZED, "토큰이 만료되었습니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));

        // 2) 사용자 찾기
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        // 주소 추가 5개로 제한
        long count = locationRepository.countByUser(user);
        if (count >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송지는 최대 5개까지만 등록할 수 있습니다.");
        }

        // 3) 바디 파싱
        String title = (String) body.getOrDefault("title", "");
        String name   = (String) body.getOrDefault("name", "");
        String phone  = (String) body.getOrDefault("phone", "");
        String region = (String) body.getOrDefault("region", "");
        String addr2  = (String) body.getOrDefault("addr2", "");
        String zipcode = (String) body.getOrDefault("zipcode", "");
        Boolean primary = (Boolean) body.getOrDefault("primary", false);

        // 4) 대표로 저장하려는 경우 → 이 유저의 기존 주소를 전부 일반으로
        // 우리 테이블: loc_default = false(0) → 대표, true(1) → 일반
        if (Boolean.TRUE.equals(primary)) {
            List<LocationEntity> oldList = locationRepository.findByUser(user);
            for (LocationEntity loc : oldList) {
                loc.setLocDefault(true);   // 1 = 일반
            }
            locationRepository.saveAll(oldList);
        }

        // 5) 새 주소 생성
        LocationEntity newLoc = LocationEntity.builder()
                .user(user)
                .locAddress(region)
                .locDetail(addr2)
                .locCode(zipcode)
                .locDefault(!Boolean.TRUE.equals(primary)) // 대표면 false(0)
                .locTitle(title)
                .locName(name)
                .locNum(phone)
                .build();

        locationRepository.save(newLoc);

        // 6) 저장 후 이 유저의 주소 전체 다시 내려주기
        List<LocationEntity> locations = locationRepository.findByUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "주소가 저장되었습니다.");
        result.put("locations", locations.stream().map(loc -> Map.of(
                "locKey", loc.getLocKey(),
                "locAddress", loc.getLocAddress(),
                "locDetail", loc.getLocDetail(),
                "locCode", loc.getLocCode(),
                "locDefault", loc.isLocDefault(),
                "locTitle", loc.getLocTitle(),
                "locName", loc.getLocName(),
                "locNum", loc.getLocNum()
        )).toList());

        return ResponseEntity.ok(result);
    }

    //삭제
//    @Transactional
//    public ResponseEntity<?> deleteLocation(HttpServletRequest request, Long locKey) {
//        // 1) 토큰 꺼내기
//        String auth = request.getHeader("Authorization");
//        if (auth == null || !auth.startsWith("Bearer ")) {
//            throw new ResponseStatusException(UNAUTHORIZED, "토큰이 없습니다.");
//        }
//        String token = auth.substring(7);
//        if (jwtProvider.isExpired(token)) {
//            throw new ResponseStatusException(UNAUTHORIZED, "토큰이 만료되었습니다.");
//        }
//
//        Long uIdx = Long.valueOf(jwtProvider.getUid(token));
//
//        // 2) 사용자 찾기
//        UserEntity user = userRepository.findById(uIdx)
//                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));
//
//        // 삭제할 배송지
//        LocationEntity location = locationRepository.findById(locKey)
//    }

}
