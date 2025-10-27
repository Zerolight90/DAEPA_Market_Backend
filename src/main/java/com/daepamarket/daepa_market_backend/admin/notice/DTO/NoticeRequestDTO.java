package com.daepamarket.daepa_market_backend.admin.notice.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NoticeRequestDTO {

    private Long adIdx;    // 작성 관리자 idx (프론트에서 전달)
    private String nSubject;
    private String nContent;
    private String nImg;
    private String nIp;
    private Byte nCategory;
}