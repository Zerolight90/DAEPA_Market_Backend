package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.admin.review.SaleReviewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
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

    @PatchMapping("/users/{uIdx}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long uIdx,
            @RequestBody UserUpdateDTO dto
    ) {
        adminUserService.updateUser(uIdx, dto);
        return ResponseEntity.ok(Map.of("message", "사용자 정보가 수정되었습니다."));
    }

}