package com.daepamarket.daepa_market_backend.domain.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserLoginDTO {

    private Long u_idx;

    private String u_id;

    private String u_pw;

    private String message;


}
