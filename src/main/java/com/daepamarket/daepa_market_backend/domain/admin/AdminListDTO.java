package com.daepamarket.daepa_market_backend.domain.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminListDTO {
    private Long adIdx;
    private String adId;
    private String adName;
    private String adNick;
    private Integer adStatus;
    private String adBirth;
}

