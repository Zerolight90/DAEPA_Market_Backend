package com.daepamarket.daepa_market_backend.delivery;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userDeliveryController")
@RequestMapping("/api/delivery")
@RequiredArgsConstructor

public class deliveryController {
    private final deliveryService deliveryService;

    // 보낸 택배
    @GetMapping("/sent")
    public ResponseEntity<?> getSentDeliveries(HttpServletRequest request) {
        return deliveryService.getMySentDeliveries(request);
    }

    // 받은 택배
    @GetMapping("/received")
    public ResponseEntity<?> getReceived(HttpServletRequest request) {
        return deliveryService.getMyReceivedDeliveries(request);
    }

}
