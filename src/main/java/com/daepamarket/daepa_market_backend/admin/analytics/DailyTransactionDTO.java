package com.daepamarket.daepa_market_backend.admin.analytics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyTransactionDTO {
    private String date;            // 날짜 (MM/dd 형식)
    private Integer value;          // 거래 건수
    private Long totalAmount;       // 총 금액
    private Integer sellerCount;    // 판매자 수
}