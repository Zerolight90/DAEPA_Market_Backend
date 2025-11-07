package com.daepamarket.daepa_market_backend.admin.check;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/checks")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class CheckController {

    private final CheckService checkService;

    // 전체 조회
    @GetMapping
    public List<CheckDTO> getAll() {
        return checkService.getAllChecks();
    }

    // 검수 결과 등록 / 수정
    @PatchMapping("/{id}")
    public ResponseEntity<String> updateResult(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body
    ) {
        checkService.updateCheckResult(id, body.get("result"));
        return ResponseEntity.ok("검수 결과가 업데이트되었습니다.");
    }
}
