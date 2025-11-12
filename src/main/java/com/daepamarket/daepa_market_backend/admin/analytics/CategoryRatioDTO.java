package com.daepamarket.daepa_market_backend.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRatioDTO {
    private String category;
    private Long count;
}

