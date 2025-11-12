package com.daepamarket.daepa_market_backend.admin.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateDTO {
    private String uname;
    private String unickname;
    private String uphone;
    private String ubirth;
    private String ugender;
    private Integer ustatus;
    private Integer uwarn;
    private String loc_address;
    private String loc_detail;
    private Double umanner;
}

