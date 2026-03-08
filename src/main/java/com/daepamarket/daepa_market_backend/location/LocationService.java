package com.daepamarket.daepa_market_backend.location;

import com.daepamarket.daepa_market_backend.domain.location.LocationDto;
import com.daepamarket.daepa_market_backend.domain.location.LocationEntity;
import com.daepamarket.daepa_market_backend.domain.location.LocationRepository;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil; // ✅ 의존성 주입 확인

    @Transactional
    public ResponseEntity<?> addLocation(HttpServletRequest request, Map<String, Object> body) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        long count = locationRepository.countByUser(user);
        if (count >= 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송지는 최대 5개까지만 등록할 수 있습니다.");
        }

        String title = (String) body.getOrDefault("title", "");
        String name = (String) body.getOrDefault("name", "");
        String phone = (String) body.getOrDefault("phone", "");
        String region = (String) body.getOrDefault("region", "");
        String addr2 = (String) body.getOrDefault("addr2", "");
        String zipcode = (String) body.getOrDefault("zipcode", "");
        Boolean primary = (Boolean) body.getOrDefault("primary", false);

        if (Boolean.TRUE.equals(primary)) {
            List<LocationEntity> oldList = locationRepository.findByUser(user);
            for (LocationEntity loc : oldList) {
                loc.setLocDefault(true);   // 1 = 일반
            }
            locationRepository.saveAll(oldList);
        }

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

        List<LocationEntity> locations = locationRepository.findByUser(user);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "주소가 저장되었습니다.");
        
        // 🚨 Map.of() 대신 HashMap 사용하여 null 허용!
        result.put("locations", locations.stream().map(loc -> {
            Map<String, Object> locMap = new HashMap<>();
            locMap.put("locKey", loc.getLocKey());
            locMap.put("locAddress", loc.getLocAddress());
            locMap.put("locDetail", loc.getLocDetail()); // null 방지
            locMap.put("locCode", loc.getLocCode());
            locMap.put("locDefault", loc.isLocDefault());
            locMap.put("locTitle", loc.getLocTitle());
            locMap.put("locName", loc.getLocName());
            locMap.put("locNum", loc.getLocNum());
            return locMap;
        }).toList());

        return ResponseEntity.ok(result);
    }

    @Transactional
    public ResponseEntity<?> deleteLocation(HttpServletRequest request, Long locKey) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        LocationEntity location = locationRepository.findById(locKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송지를 찾을 수 없습니다."));

        if (!location.isLocDefault()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대표 배송지는 삭제할 수 없습니다.");
        }

        locationRepository.delete(location);
        return ResponseEntity.ok(Map.of("message", "배송지가 삭제되었습니다."));
    }

    @Transactional
    public ResponseEntity<?> updateLocation(HttpServletRequest request, Long locKey) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(token));
        UserEntity user = userRepository.findById(uIdx)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "사용자를 찾을 수 없습니다."));

        LocationEntity location = locationRepository.findById(locKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "배송지를 찾을 수 없습니다."));

        List<LocationEntity> oldList = locationRepository.findByUser(user);
        for (LocationEntity loc : oldList) {
            loc.setLocDefault(true); // 일반
        }

        location.setLocDefault(false);
        locationRepository.saveAll(oldList);

        List<LocationEntity> updated = locationRepository.findByUser(user);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "대표 배송지가 변경되었습니다.");
        
        // 🚨 Map.of() 대신 HashMap 사용하여 null 허용!
        responseBody.put("locations", updated.stream().map(loc -> {
            Map<String, Object> locMap = new HashMap<>();
            locMap.put("locKey", loc.getLocKey());
            locMap.put("locAddress", loc.getLocAddress());
            locMap.put("locDetail", loc.getLocDetail()); // null 방지
            locMap.put("locCode", loc.getLocCode());
            locMap.put("locDefault", loc.isLocDefault());
            locMap.put("locTitle", loc.getLocTitle());
            locMap.put("locName", loc.getLocName());
            locMap.put("locNum", loc.getLocNum());
            return locMap;
        }).toList());

        return ResponseEntity.ok(responseBody);
    }

    public LocationDto getDefaultLocation(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return locationRepository.findByUserAndLocDefault(user, false)
                .map(LocationDto::new)
                .orElse(null);
    }

    public List<LocationDto> getAllLocations(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return locationRepository.findByUser(user).stream()
                .map(LocationDto::new)
                .collect(Collectors.toList());
    }
}