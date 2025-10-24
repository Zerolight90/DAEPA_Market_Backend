package com.daepamarket.daepa_market_backend.domain.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateDTO {
    private Long adIdx;       // 세션에 있는 값 그대로 전달 (본인 수정)
    private String adNick;
    // private String adName;
    private String adBirth; // yyyy-MM-dd 문자열
    private String adId;
    private String newPassword; // 비번 변경 시만 전달, 빈문자열/NULL이면 변경 안 함
}