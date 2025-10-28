package com.daepamarket.daepa_market_backend.admin.notice.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class NoticeRequestDTO {

    @JsonProperty("adIdx")
    private Long adIdx;    // 작성 관리자 idx (프론트에서 전달)

    @JsonProperty("nSubject")
    private String nSubject;

    @JsonProperty("nContent")
    private String nContent;

    @JsonProperty("nImg")
    private String nImg;

    @JsonProperty("nIp")
    private String nIp;

    @JsonProperty("nCategory")
    private Byte nCategory;
}