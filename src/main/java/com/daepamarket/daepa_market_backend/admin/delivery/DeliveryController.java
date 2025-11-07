package com.daepamarket.daepa_market_backend.admin.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController("adminDeliveryController")
@RequiredArgsConstructor
@RequestMapping("/api/admin/deliveries")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class DeliveryController {

    private final AdminDeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<List<DeliveryDTO>> getAllDeliveries() {
        return ResponseEntity.ok(deliveryService.getAllDeliveries());
    }

    @PatchMapping("/{dvIdx}/status")
    public ResponseEntity<?> updateDeliveryStatus(
            @PathVariable Long dvIdx,
            @RequestBody Map<String, Integer> body
    ) {
        deliveryService.updateDeliveryStatus(dvIdx, body.get("status"));
        return ResponseEntity.ok("배송 상태가 업데이트되었습니다.");
    }
}

