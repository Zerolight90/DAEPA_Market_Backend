package com.daepamarket.daepa_market_backend.admin.naga;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StopDTO {
    private String suspendDate; // YYYY-MM-DD
    private String reason;      // 정지사유
    private String duration;    // 1일 / 3일 / 무기한 등
}
