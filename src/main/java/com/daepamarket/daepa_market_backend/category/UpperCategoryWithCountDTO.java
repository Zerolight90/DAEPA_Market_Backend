package com.daepamarket.daepa_market_backend.category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpperCategoryWithCountDTO {

    private Long upperIdx;
    private String upperCt;
    private Long productCount;
}
