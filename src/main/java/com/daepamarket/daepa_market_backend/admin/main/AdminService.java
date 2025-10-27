package com.daepamarket.daepa_market_backend.admin.main;

import com.daepamarket.daepa_market_backend.domain.admin.AdminDTO;
import com.daepamarket.daepa_market_backend.domain.admin.AdminEntity;
import com.daepamarket.daepa_market_backend.domain.admin.AdminRepository;
import com.daepamarket.daepa_market_backend.domain.admin.AdminUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminDTO getMyProfile(Long adIdx) {
        AdminEntity admin = adminRepository.findById(adIdx)
                .orElseThrow(() -> new RuntimeException("관리자 정보를 찾을 수 없습니다."));

        return toDto(admin);
    }

    public AdminDTO updateMyProfile(AdminUpdateDTO req) {
        AdminEntity admin = adminRepository.findById(req.getAdIdx())
                .orElseThrow(() -> new RuntimeException("관리자 정보를 찾을 수 없습니다."));

        // ID(adId)는 수정 불가 정책
        admin.setAdNick(req.getAdNick());
        // admin.setAdName(req.getAdName());

        if (req.getAdBirth() != null && !req.getAdBirth().isBlank()) {
            admin.setAdBirth(LocalDate.parse(req.getAdBirth()));
        } else {
            admin.setAdBirth(null);
        }

        // 비밀번호 변경 요청 시: 평문 그대로 저장 (요청 조건)
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            admin.setAdPw(req.getNewPassword());
        }

        AdminEntity saved = adminRepository.save(admin);
        return toDto(saved);
    }

    private AdminDTO toDto(AdminEntity e) {
        AdminDTO dto = new AdminDTO();
        dto.setAdIdx(e.getAdIdx());
        dto.setAdId(e.getAdId());
        dto.setAdNick(e.getAdNick());
        dto.setAdName(e.getAdName());
        dto.setAdBirth(e.getAdBirth() != null ? e.getAdBirth().toString() : null);
        dto.setAdStatus(e.getAdStatus());
        return dto;
    }
}
