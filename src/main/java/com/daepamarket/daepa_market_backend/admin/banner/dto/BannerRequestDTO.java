package com.daepamarket.daepa_market_backend.admin.banner.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BannerRequestDTO {
    private String title;
    private String subtitle;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean active;
}

