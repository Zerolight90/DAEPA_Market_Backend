package com.daepamarket.daepa_market_backend.admin.main;

import com.daepamarket.daepa_market_backend.admin.analytics.AnalyticsService;
import com.daepamarket.daepa_market_backend.admin.analytics.DailyTransactionDTO;
import com.daepamarket.daepa_market_backend.admin.analytics.DashboardStatsDTO;
import com.daepamarket.daepa_market_backend.admin.product.AdminProductService;
import com.daepamarket.daepa_market_backend.admin.product.dto.AdminProductPageResponse;
import com.daepamarket.daepa_market_backend.admin.user.UserResponseDTO;
import com.daepamarket.daepa_market_backend.domain.admin.*;
import com.daepamarket.daepa_market_backend.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class AdminController {

    private final AdminRepository adminRepository;
    private final UserService userService;
    private final AdminService adminService;
    private final AnalyticsService analyticsService;
    private final AdminProductService adminProductService;

    @PostMapping("/add-admin")
    public String addAdmin( @RequestBody AdminDTO request) {
        if (request.getAdId() == null || request.getAdPw() == null ||
                request.getAdName() == null || request.getAdBirth() == null) {
            return "필수 항목이 누락되었습니다.";
        }

        AdminEntity admin = AdminEntity.builder()
                .adId(request.getAdId())
                .adName(request.getAdName())
                .adPw(request.getAdPw())
                .adNick(request.getAdNick())
                .adBirth(LocalDate.parse(request.getAdBirth()))
                .adStatus(request.getAdStatus() != null ? request.getAdStatus() : 1) // 기본값: 활성화
                .build();

        adminRepository.save(admin);
        return "관리자 추가 성공";
    }

    // 회원 목록 조회
    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        return userService.findAllUsers();
    }

    // 관리자 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login( @RequestBody AdminLoginDTO req) {
        log.info(">>> Admin Login 시도: ID=[{}], PW=[{}]", req.getAdminId(), req.getPassword());

        AdminEntity admin = adminRepository.findByAdId(req.getAdminId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 관리자 ID입니다."));

        // 지금은 평문 정책이므로 그냥 equals로 비교
        if (!req.getPassword().equals(admin.getAdPw())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return ResponseEntity.ok(new HashMap<>() {{
            put("adIdx", admin.getAdIdx());
            put("adNick", admin.getAdNick());
        }});
    }

    @GetMapping("/me")
    public ResponseEntity<AdminDTO> getMyProfile( @RequestParam Long adIdx) {
        return ResponseEntity.ok(adminService.getMyProfile(adIdx));
    }

    @PutMapping("/me")
    public ResponseEntity<AdminDTO> updateMyProfile( @RequestBody AdminUpdateDTO req) {
        return ResponseEntity.ok(adminService.updateMyProfile(req));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<com.daepamarket.daepa_market_backend.domain.admin.AdminListDTO>> getAdmins() {
        return ResponseEntity.ok(adminService.getAdminList());
    }

    @GetMapping("/products")
    public ResponseEntity<AdminProductPageResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String status
    ) {
        var result = adminProductService.getProducts(page, size, status);
        AdminProductPageResponse response = new AdminProductPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        adminProductService.softDeleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    // 일간 거래 추이 (주간)
    @GetMapping("/analytics/daily-transactions")
    public ResponseEntity<List<DailyTransactionDTO>> getDailyTransactions() {
        return ResponseEntity.ok(analyticsService.getWeeklyTransactionTrend());
    }

    // 대시보드 통계
    @GetMapping("/analytics/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(analyticsService.getDashboardStats());
    }

    // 최근 등록 상품 조회
    @GetMapping("/analytics/recent-products")
    public ResponseEntity<List<com.daepamarket.daepa_market_backend.admin.analytics.RecentProductDTO>> getRecentProducts(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analyticsService.getRecentProducts(limit));
    }

    @GetMapping("/analytics/category-ratio")
    public ResponseEntity<List<com.daepamarket.daepa_market_backend.admin.analytics.CategoryRatioDTO>> getCategoryRatio() {
        return ResponseEntity.ok(analyticsService.getCategoryRatios());
    }
}