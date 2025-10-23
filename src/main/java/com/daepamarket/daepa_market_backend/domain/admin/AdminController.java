package com.daepamarket.daepa_market_backend.domain.admin;

import com.daepamarket.daepa_market_backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*") // Next.js 프론트와 통신 허용
public class AdminController {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserService userService;

    @PostMapping("/add-admin")
    public String addAdmin(@RequestBody AdminDTO request) {
        if (request.getAdId() == null || request.getAdPw() == null ||
                request.getAdName() == null || request.getAdBirth() == null) {
            return "필수 항목이 누락되었습니다.";
        }

        AdminEntity admin = AdminEntity.builder()
                .adId(request.getAdId())
                .adName(request.getAdName())
                .adName(request.getAdPw())
                .adNick(request.getAdNick())
                .adBirth(LocalDate.parse(request.getAdBirth()))
                .adStatus(request.getAdStatus() != null ? request.getAdStatus() : 1) // 기본값: 활성화
                .build();

        adminRepository.save(admin);
        return "관리자 추가 성공";
    }

    @GetMapping("/users")
    public List<UserResponseDTO> getAllUsers() {
        return userService.findAllUsers();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginDTO req) {
        AdminEntity admin = adminRepository.findByAdId(req.getAdminId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 관리자 ID입니다."));

        if (!req.getPassword().equals(admin.getAdPw())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return ResponseEntity.ok(new HashMap<>() {{
            put("adIdx", admin.getAdIdx());
            put("adNick", admin.getAdNick());
        }});
    }

}
