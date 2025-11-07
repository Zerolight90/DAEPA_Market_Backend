package com.daepamarket.daepa_market_backend.admin.naga;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NagaReportDTO {
    private Long id;              // ng_idx
    private String reporterName;  // s_idx -> 닉네임 변환 예정
    private String reportedName;  // b_idx2 -> 닉네임 변환 예정
    private String type;          // 신고 종류
    private String content;       // 신고 내용
    private String createdAt;     // 신고 날짜
    private String status;        // 처리 상태: "pending", "suspended", "activated", "banned"
    private String actionType;    // 조치 유형: "suspend", "activate", "ban"
}
