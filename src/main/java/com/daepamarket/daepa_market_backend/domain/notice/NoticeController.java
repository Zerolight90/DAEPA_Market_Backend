package com.daepamarket.daepa_market_backend.domain.notice;

import com.daepamarket.daepa_market_backend.domain.notice.NoticeRequestDTO;
import com.daepamarket.daepa_market_backend.domain.notice.NoticeResponseDTO;
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

    /** 공지 작성 (DTO로 받고 DTO로 반환) */
    @PostMapping
    public NoticeResponseDTO create(@RequestBody NoticeRequestDTO req) {
        return noticeService.createNotice(req);
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
}
