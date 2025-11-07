package com.daepamarket.daepa_market_backend.admin.oneonone.Controller;

import com.daepamarket.daepa_market_backend.admin.oneonone.DTO.OneOnOneResponseDTO;
import com.daepamarket.daepa_market_backend.admin.oneonone.Service.OneOnOneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/contact")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class OneOnOneController {

    private final OneOnOneService service;

    @GetMapping
    public List<OneOnOneResponseDTO> getList() {
        return service.getList().stream().map(i -> OneOnOneResponseDTO.builder()
                .id(i.getOoIdx())
                .name(i.getUser() != null ? i.getUser().getUname() : "")
                .title(i.getOoTitle())
                .content(i.getOoContent())
                .photo(i.getOoPhoto())
                .category(i.getOoStatus()) // 문의 유형(숫자)
                .date(i.getOoDate())
                .status((i.getOoRe() == null || i.getOoRe().isBlank()) ? "pending" : "completed")
                .build()
        ).toList();
    }

    @GetMapping("/{id}")
    public OneOnOneResponseDTO getDetail(@PathVariable Long id) {
        var i = service.getById(id);
        return OneOnOneResponseDTO.builder()
                .id(i.getOoIdx())
                .name(i.getUser() != null ? i.getUser().getUname() : "")
                .title(i.getOoTitle())
                .content(i.getOoContent())
                .photo(i.getOoPhoto())
                .category(i.getOoStatus())
                .date(i.getOoDate())
                .status((i.getOoRe() == null || i.getOoRe().isBlank()) ? "pending" : "completed")
                .reply(i.getOoRe() != null ? i.getOoRe() : "")
                .build();
    }

    // 답변 등록 기능 (JSON {"reply": "..."} 형태 지원)
    @PostMapping("/{id}/reply")
    public ResponseEntity<String> addReply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object raw = body.get("reply");
        String replyText = raw == null ? null : String.valueOf(raw);
        service.addReply(id, replyText);
        return ResponseEntity.ok("답변이 저장되었습니다.");
    }
}
