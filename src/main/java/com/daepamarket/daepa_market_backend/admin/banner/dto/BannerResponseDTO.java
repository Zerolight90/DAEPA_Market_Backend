package com.daepamarket.daepa_market_backend.admin.banner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BannerResponseDTO {
    private final Long id;
    private final String title;
    private final String subtitle;
    private final String imageUrl;
    private final Integer displayOrder;
    private final Boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}

