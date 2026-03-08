package com.daepamarket.daepa_market_backend.location;

import com.daepamarket.daepa_market_backend.domain.location.LocationDto;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/sing")
public class LocationController {
    private final LocationService locationService;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;    

    @PostMapping("/location")
    public ResponseEntity<?> addLocation(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> body){
        return ResponseEntity.ok(Map.of("message", "배송지가 등록되었습니다.", "locations", locationService.addLocation(request, body)));

    }

    // 삭제
    @DeleteMapping("/location/{locKey}")
    public ResponseEntity<?> deleteLocation(HttpServletRequest request, @PathVariable Long locKey) {
        locationService.deleteLocation(request, locKey);
        return ResponseEntity.ok(Map.of("message", "배송지가 삭제되었습니다."));
    }

    @PutMapping("/location/{locKey}/update")
    public ResponseEntity<?> updateLocation(HttpServletRequest request, @PathVariable Long locKey) {
        return locationService.updateLocation(request, locKey);

    }

    // =================================================================
    // 새로 추가되는 API 엔드포인트
    // =================================================================

    /**
     * 현재 로그인된 사용자의 기본 배송지를 조회합니다.
     */
    @GetMapping("/locations/default")
    public ResponseEntity<LocationDto> getDefaultLocation(HttpServletRequest request) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new SecurityException("유효하지 않은 토큰입니다."); // (또는 리턴값에 맞게 변경)
        }
        Long userId = Long.valueOf(jwtProvider.getUid(token));

        LocationDto locationDto = locationService.getDefaultLocation(userId);
        if (locationDto == null) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.ok(locationDto);
    }

    /**
     * 현재 로그인된 사용자의 모든 배송지 목록을 조회합니다.
     */
    @GetMapping("/locations")
    public ResponseEntity<List<LocationDto>> getAllLocations(HttpServletRequest request) {
        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new SecurityException("유효하지 않은 토큰입니다."); // (또는 리턴값에 맞게 변경)
        }
        Long uIdx = Long.valueOf(jwtProvider.getUid(token));

        List<LocationDto> locations = locationService.getAllLocations(uIdx);
        return ResponseEntity.ok(locations);
    }
}