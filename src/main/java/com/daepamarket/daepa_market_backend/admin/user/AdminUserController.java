package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.admin.review.SaleReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/users/{uIdx}")
    public ResponseEntity<UserDetailDTO> getUserDetail(@PathVariable Long uIdx) {
        return ResponseEntity.ok(adminUserService.getUserDetail(uIdx));
    }

    @PatchMapping("/users/{uIdx}/manner")
    public ResponseEntity<?> updateUserManner(
            @PathVariable Long uIdx,
            @RequestBody Map<String, Double> request
    ) {
        Double umanner = request.get("umanner");
        adminUserService.updateManner(uIdx, umanner);
        return ResponseEntity.ok().body(Map.of("umanner", umanner));
    }

    @GetMapping("/users/{userId}/reviews/sell")
    public List<SaleReviewDTO> getSellerReviews(@PathVariable Long userId) {
        return adminUserService.getUserSaleReviews(userId);
    }

}