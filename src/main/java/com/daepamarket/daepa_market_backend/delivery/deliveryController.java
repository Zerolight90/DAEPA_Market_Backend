package com.daepamarket.daepa_market_backend.delivery;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
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

    //판매 내역 배송 보냄 확인 버튼
    @PatchMapping("/{dealId}/sent")
    public ResponseEntity<?> markDeliverySent(@PathVariable Long dealId) {
        try {
            deliveryService.markAsSent(dealId);
            log.info("✅ 배송 보냄 확인 완료: dealId={}", dealId);
            return ResponseEntity.ok("배송 상태가 '보냄(1)'으로 업데이트되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("🚨 배송 상태 업데이트 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
        }
    }

    @PatchMapping("/{dealId}/done")
    public ResponseEntity<?> markDeliveryDone(@PathVariable Long dealId) {
        deliveryService.updateStatus(dealId);
        return ResponseEntity.ok("배송 단계가 완료되었습니다.");
    }

}
