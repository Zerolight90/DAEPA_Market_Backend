// src/main/java/com/daepamarket/daepa_market_backend/review/dto/ReviewCreateRequest.java
package com.daepamarket.daepa_market_backend.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter   // ✅ 이게 있어야 JSON이 들어온다
public class ReviewCreateRequest {

    @JsonProperty("dIdx")      // JSON 키: dIdx
    private Long dIdx;

    @JsonProperty("reStar")    // JSON 키: reStar
    private Integer reStar;

    @JsonProperty("reContent") // JSON 키: reContent
    private String reContent;

    @JsonProperty("reType")    // JSON 키: reType
    private String reType;
}
