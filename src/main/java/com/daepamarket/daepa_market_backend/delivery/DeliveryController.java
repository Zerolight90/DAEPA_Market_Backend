package com.daepamarket.daepa_market_backend.delivery;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // 보낸 택배 내역 (판매자 기준)
    @GetMapping("/sent")
    public ResponseEntity<?> getSent(HttpServletRequest request) {
        return deliveryService.getMySentDeliveries(request);
    }

    // 받은 택배 내역 (구매자 기준)
    @GetMapping("/received")
    public ResponseEntity<?> getReceived(HttpServletRequest request) {
        return deliveryService.getMyReceivedDeliveries(request);
    }
}
