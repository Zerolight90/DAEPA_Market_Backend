package com.daepamarket.daepa_market_backend.admin.main;

import com.daepamarket.daepa_market_backend.domain.admin.AdminDTO;
import com.daepamarket.daepa_market_backend.domain.admin.AdminEntity;
import com.daepamarket.daepa_market_backend.domain.admin.AdminListDTO;
import com.daepamarket.daepa_market_backend.domain.admin.AdminRepository;
import com.daepamarket.daepa_market_backend.domain.admin.AdminUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

        if (req.getAdId() != null && !req.getAdId().isBlank() && !req.getAdId().equals(admin.getAdId())) {
            adminRepository.findByAdId(req.getAdId()).ifPresent(dup -> {
                if (!dup.getAdIdx().equals(admin.getAdIdx())) {
                    throw new RuntimeException("이미 사용 중인 관리자 ID입니다.");
                }
            });
            admin.setAdId(req.getAdId());
        }

        admin.setAdNick(req.getAdNick());
        admin.setAdName(req.getAdName());

        if (req.getAdBirth() != null && !req.getAdBirth().isBlank()) {
            admin.setAdBirth(LocalDate.parse(req.getAdBirth()));
        } else {
            admin.setAdBirth(null);
        }

        String newPassword = req.getNewPassword() != null ? req.getNewPassword() : req.getAdPw();
        if (newPassword != null && !newPassword.isBlank()) {
            admin.setAdPw(newPassword);
        }

        AdminEntity saved = adminRepository.save(admin);
        return toDto(saved);
    }

    public List<AdminListDTO> getAdminList() {
        return adminRepository.findAll().stream()
                .map(admin -> new AdminListDTO(
                        admin.getAdIdx(),
                        admin.getAdId(),
                        admin.getAdName(),
                        admin.getAdNick(),
                        admin.getAdStatus(),
                        admin.getAdBirth() != null ? admin.getAdBirth().toString() : null
                ))
                .collect(Collectors.toList());
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
