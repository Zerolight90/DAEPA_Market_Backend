package com.daepamarket.daepa_market_backend.oneonone;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOneOnOneCreateDTO {

    // oo_status (1: 불편 신고, 2: 거래 관련, 3: 계정/로그인, 4: 기타 문의)
    private Integer status;

    // oo_title
    private String title;

    // oo_content
    private String content;

}
