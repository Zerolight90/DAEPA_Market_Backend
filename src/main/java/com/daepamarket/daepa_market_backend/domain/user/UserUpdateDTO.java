package com.daepamarket.daepa_market_backend.domain.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateDTO {
    private String nickname;        // 별명
    private String newPassword;     // 새 비번
    private String gender;          // M / F
    private String birth;           // yyyyMMdd
    private String zip;             // 우편번호
    private String address;         // 도로명
    private String addressDetail;   // 상세주소

    private String profile;
}
