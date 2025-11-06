package com.daepamarket.daepa_market_backend.location;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/sing")
public class LocationController {
    private final LocationService locationService;

    @PostMapping("/location")
    public ResponseEntity<?> addLocation(HttpServletRequest request, HttpServletResponse response, @RequestBody Map<String, Object> body){
        return ResponseEntity.ok(Map.of("message", "배송지가 등록되었습니다.", "locations", locationService.addLocation(request, body)));

    }

    // 삭제
//    @DeleteMapping("/location/{locKey}")
//    public ResponseEntity<?> deleteLocation(HttpServletRequest request, @PathVariable Long locKey) {
//        locationService.deleteLocation(request, locKey);
//        return ResponseEntity.ok(Map.of("message", "배송지가 삭제되었습니다."));
//    }

}
