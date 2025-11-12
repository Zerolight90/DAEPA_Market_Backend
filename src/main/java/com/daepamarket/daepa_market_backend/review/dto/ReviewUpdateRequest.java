package com.daepamarket.daepa_market_backend.review.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewUpdateRequest {
    private Integer reStar;     // 1~5
    private String  reContent;  // 0~500자
}
