package com.daepamarket.daepa_market_backend.admin.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProductSummaryDTO {
    private final Long id;
    private final String title;
    private final Long price;
    private final String thumbnail;
    private final String category;
    private final String createdAt;
    private final Long sellerId;
    private final String sellerName;
    private final String saleStatus;
    private final boolean reported;
}


