package com.daepamarket.daepa_market_backend.admin.notice.Controller;

import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeRequestDTO;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeResponseDTO;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeUpdateDTO;
import com.daepamarket.daepa_market_backend.admin.notice.Service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// ... imports

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000","http://3.34.181.73"})
public class NoticeController {

    private final NoticeService noticeService;

    /** 전체 목록 조회 (DTO로 반환) */
    @GetMapping
    public List<NoticeResponseDTO> list() {
        return noticeService.findAllDTO();
    }

    /** 상세 조회 (DTO로 반환) */
    @GetMapping("/{id}")
    public NoticeResponseDTO detail(@PathVariable Long id) {
        return noticeService.findByIdDTO(id);
    }

    /** 공지 수정 */
    @PutMapping("/{id}")
    public NoticeResponseDTO update(@PathVariable Long id,
                                    @RequestPart("req") NoticeUpdateDTO req,
                                    @RequestPart(value = "file", required = false) MultipartFile file) {
        return noticeService.update(id, req, file);
    }

    /** 공지 삭제 */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        noticeService.delete(id);
    }

    /** 공지 작성 */
    @PostMapping
    public NoticeResponseDTO create(HttpServletRequest request,
                                    Authentication authentication,
                                    @RequestPart("req") NoticeRequestDTO req,
                                    @RequestPart(value = "file", required = false) MultipartFile file) {

        // 1. Spring Security 컨텍스트에서 인증된 사용자 정보 가져오기
        UserEntity admin = (UserEntity) authentication.getPrincipal();
        Long adminId = admin.getUIdx();

        // 요청한 클라이언트 IP 추출
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }

        // DTO에 주입
        req.setNIp(clientIp);

        // 서비스 호출
        return noticeService.createNotice(adminId, req, file);
    }
}
