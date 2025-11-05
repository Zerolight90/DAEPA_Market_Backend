package com.daepamarket.daepa_market_backend.domain.user;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UserSignUpDTO {

    // 이메일을 u_id에 저장
    private String u_id;

    private String u_pw;

    private String u_name;

    private String u_nickname;

    private String u_phone;

    private String u_address;

    private String u_location;

    private String u_location_detail;

    private String u_locCode;

    private String u_birth;

    private String u_gender;

    private String u_agree;
}
