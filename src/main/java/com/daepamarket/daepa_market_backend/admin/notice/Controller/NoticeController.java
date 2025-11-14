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

        // 1. 토큰에서 관리자 ID 추출
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new SecurityException("토큰이 없거나 형식이 올바르지 않습니다.");
        }
        String token = auth.substring(7);
        Long adminId = Long.valueOf(jwtProvider.getUid(token));

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
