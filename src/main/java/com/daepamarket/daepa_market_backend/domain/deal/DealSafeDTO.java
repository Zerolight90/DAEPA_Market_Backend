package com.daepamarket.daepa_market_backend.domain.deal;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
@AllArgsConstructor
public class DealSafeDTO {
    private String productTitle;   // product.pd_title
    private Long agreedPrice;      // deal.agreed_price
    private Timestamp dealDate; // deal.dEdate
    private Long dStatus;
}
