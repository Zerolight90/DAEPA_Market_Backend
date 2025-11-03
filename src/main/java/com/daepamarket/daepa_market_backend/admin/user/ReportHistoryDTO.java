package com.daepamarket.daepa_market_backend.admin.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class ReportHistoryDTO {
    private Long id;
    private String reporter;
    private String content;
    private Object date;   // LocalDate든 String이든 받도록
    private Integer type;
}
