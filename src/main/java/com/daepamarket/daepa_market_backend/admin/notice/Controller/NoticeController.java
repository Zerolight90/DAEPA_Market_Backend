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
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
public class NoticeController {

    private final NoticeService noticeService;
    private final com.daepamarket.daepa_market_backend.jwt.JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

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
        String token = cookieUtil.getAccessTokenFromCookie(request); // auth 변수 대신 바로 token으로 받기
        if (token == null || token.isBlank()) { 
            throw new SecurityException("토큰이 없습니다."); 
        }

        // Bearer 자르는 로직 없이 곧바로 만료 확인 및 파싱
        if (jwtProvider.isExpired(token)) {
            throw new SecurityException("유효하지 않은 토큰입니다.");
        }
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
