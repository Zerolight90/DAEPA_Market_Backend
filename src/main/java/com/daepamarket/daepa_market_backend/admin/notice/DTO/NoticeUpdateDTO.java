package com.daepamarket.daepa_market_backend.admin.notice.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoticeUpdateDTO {

    @JsonProperty("nSubject")
    private String nSubject;

    @JsonProperty("nContent")
    private String nContent;

    @JsonProperty("nCategory")
    private Byte nCategory;

    @JsonProperty("nImg")
    private String nImg;

    @JsonProperty("nFix")
    private Byte nFix;
}
