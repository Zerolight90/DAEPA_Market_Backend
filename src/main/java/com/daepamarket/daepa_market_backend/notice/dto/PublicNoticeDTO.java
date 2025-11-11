package com.daepamarket.daepa_market_backend.notice.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class PublicNoticeDTO {
    private Long nIdx;
    private String nSubject;
    private String nContent;
    private String adminNick;
    private LocalDate nDate;
    private String nImg;
    private Byte nCategory; // Changed from nCategoryName to nCategory
    private Byte nFix;
}
