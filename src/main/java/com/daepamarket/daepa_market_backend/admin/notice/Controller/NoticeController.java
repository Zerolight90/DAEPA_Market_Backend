package com.daepamarket.daepa_market_backend.admin.notice.Controller;

import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeRequestDTO;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeResponseDTO;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeUpdateDTO;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeEntity;
import com.daepamarket.daepa_market_backend.admin.notice.Service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000","http://3.34.181.73","https://daepamarket.shop"})
public class NoticeController {

    private final NoticeService noticeService;
    private final com.daepamarket.daepa_market_backend.jwt.JwtProvider jwtProvider;

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
                                    @RequestPart("req") NoticeRequestDTO req,
                                    @RequestPart(value = "file", required = false) MultipartFile file) {

        // 1. 토큰에서 관리자 ID 추출 (수동 인증 로직 제거)
        // Long adminId = Long.valueOf(jwtProvider.getUid(token)); // 기존 코드

        System.out.println("🔥 create() 진입: /api/admin/notices POST");  // 로그 추가
        Long adminId = 1L; // 임시 관리자 ID (보안 취약, 추후 수정 필요)

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
