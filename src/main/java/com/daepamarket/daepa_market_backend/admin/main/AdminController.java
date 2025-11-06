package com.daepamarket.daepa_market_backend.admin.main;

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
@RestController @RequiredArgsConstructor @RequestMapping("/api/admin")
public class AdminController {

    private final AdminRepository adminRepository;
    private final UserService userService;
    private final AdminService adminService;

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

}