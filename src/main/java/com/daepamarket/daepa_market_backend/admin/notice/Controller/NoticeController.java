package com.daepamarket.daepa_market_backend.admin.notice.Controller;

import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeRequestDTO;
import com.daepamarket.daepa_market_backend.admin.notice.DTO.NoticeResponseDTO;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeEntity;
import com.daepamarket.daepa_market_backend.admin.notice.Service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
@CrossOrigin(origins = "*")
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

    /** 공지 수정 (현재는 Entity 기반, 추후 RequestDTO로 리팩토링 가능) */
    @PutMapping("/{id}")
    public NoticeResponseDTO update(@PathVariable Long id, @RequestBody NoticeEntity req) {
        return noticeService.update(id, req);
    }

    /** 공지 삭제 */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        noticeService.delete(id);
    }

    /** 공지 작성 */
    @PostMapping
    public NoticeResponseDTO create(HttpServletRequest request, @RequestBody NoticeRequestDTO req) {

        // 요청한 클라이언트 IP 추출
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        }

        // DTO에 주입
        req.setNIp(clientIp);

        // 서비스 호출
        return noticeService.createNotice(req);
    }
}
