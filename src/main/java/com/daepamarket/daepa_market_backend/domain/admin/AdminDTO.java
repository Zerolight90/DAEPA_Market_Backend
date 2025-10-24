package com.daepamarket.daepa_market_backend.domain.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {
    private Long adIdx;
    private String adId;
    private String adPw;
    private String adName;
    private String adNick;
    private String adBirth; // 문자열로 받아서 LocalDate로 변환
    private Integer adStatus; // 1=활성, 0=비활성
}
