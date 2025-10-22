package com.daepamarket.daepa_market_backend.domain.admin;

import com.daepamarket.daepa_market_backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
}
