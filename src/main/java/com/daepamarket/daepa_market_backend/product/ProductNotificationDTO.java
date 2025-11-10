package com.daepamarket.daepa_market_backend.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProductNotificationDTO {
    private Long productId;
    private String productName;
}
