package com.daepamarket.daepa_market_backend.admin.naga;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class NagaReportController {

    private final NagaReportService reportService;

    @GetMapping
    public ResponseEntity<List<NagaReportDTO>> getReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @PostMapping("/{reportId}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long reportId, @RequestBody StopDTO dto) {
        reportService.suspendUser(reportId, dto);
        return ResponseEntity.ok("정지 처리 완료");
    }

    @PostMapping("/{reportId}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long reportId) {
        reportService.activateUser(reportId);
        return ResponseEntity.ok("사용자 활성화 완료");
    }

    @PostMapping("/{reportId}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long reportId, @RequestBody GetOutDTO dto) {
        reportService.banUser(reportId, dto);
        return ResponseEntity.ok("탈퇴 처리 완료");
    }

    /** ✅ 신선도 하락(-5) */
    @PostMapping("/{reportId}/manner-down")
    public ResponseEntity<?> mannerDown(@PathVariable Long reportId) {
        reportService.decreaseManner(reportId);
        return ResponseEntity.ok("신선도를 5 하락시켰습니다.");
    }
}
