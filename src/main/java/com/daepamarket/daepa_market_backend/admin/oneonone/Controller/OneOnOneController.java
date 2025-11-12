package com.daepamarket.daepa_market_backend.admin.oneonone.Controller;

import com.daepamarket.daepa_market_backend.admin.oneonone.DTO.OneOnOneResponseDTO;
import com.daepamarket.daepa_market_backend.admin.oneonone.Service.OneOnOneService;
import com.daepamarket.daepa_market_backend.domain.oneonone.OneOnOneEntity;
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

    private static final int STATUS_OFFSET = 100;

    private final OneOnOneService service;

    private OneOnOneResponseDTO toResponseDTO(OneOnOneEntity entity) {
        int rawStatus = entity.getOoStatus() == null ? 0 : entity.getOoStatus();
        boolean completed = rawStatus >= STATUS_OFFSET;
        int category = completed ? rawStatus - STATUS_OFFSET : rawStatus;
        if (category < 0) {
            category = 0;
        }

        return OneOnOneResponseDTO.builder()
                .id(entity.getOoIdx())
                .name(entity.getUser() != null ? entity.getUser().getUname() : "")
                .title(entity.getOoTitle())
                .content(entity.getOoContent())
                .photo(entity.getOoPhoto())
                .category(category)
                .date(entity.getOoDate())
                .status(completed ? "completed" : "pending")
                .reply(entity.getOoRe() != null ? entity.getOoRe() : "")
                .build();
    }

    @GetMapping
    public List<OneOnOneResponseDTO> getList() {
        return service.getList().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public OneOnOneResponseDTO getDetail(@PathVariable Long id) {
        return toResponseDTO(service.getById(id));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<String> addReply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object raw = body.get("reply");
        if (raw == null && body.containsKey("replyContent")) {
            raw = body.get("replyContent");
        }
        String replyText = raw == null ? null : String.valueOf(raw);
        service.addReply(id, replyText);
        return ResponseEntity.ok("답변이 저장되었습니다.");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object raw = body.get("status");
        if (raw == null) {
            throw new IllegalArgumentException("status 값이 필요합니다.");
        }
        service.updateStatus(id, String.valueOf(raw));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
