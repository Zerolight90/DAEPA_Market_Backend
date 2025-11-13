// src/main/java/com/daepamarket/daepa_market_backend/naga/dto/NagaReportRequest.java
package com.daepamarket.daepa_market_backend.naga.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NagaReportRequest {
    private Long productId;   // 신고 대상 상품
    private Integer ngStatus; // 1:사기의심, 2:욕설/비방, 3:스팸/광고, 4:기타
    private String ngContent; // 상세 내용(최대 400자)
}
