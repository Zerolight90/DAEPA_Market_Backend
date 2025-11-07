package com.daepamarket.daepa_market_backend.admin.delivery;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDTO {
    private Long dvIdx;           // 배송 ID
    private Long dIdx;            // 거래 ID
    private String productName;   // 상품명
    private String sellerName;    // 판매자명
    private String buyerName;     // 구매자명
    private String address;       // 배송지 주소
    private String addressDetail; // 배송지 상세주소
    private Integer dvStatus;     // 배송 상태
    private String tradeType;     // 거래 종류
    private String dealDate;      // 거래일
}

