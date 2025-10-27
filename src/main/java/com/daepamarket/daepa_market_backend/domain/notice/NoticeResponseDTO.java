package com.daepamarket.daepa_market_backend.domain.notice;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NoticeResponseDTO {

    private Long nIdx;
    private String nSubject;
    private String nContent;
    private String nImg;
    private String nDate;
    private String nIp;
    private Byte nCategory;

}