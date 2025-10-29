package com.daepamarket.daepa_market_backend.pay;

import lombok.Getter;
import lombok.NoArgsConstructor;

// ✅ 페이 구매 요청 DTO (새로 생성)
@Getter
@NoArgsConstructor
public class PayRequestDTO {
    private Long itemId;
    private int qty;
    private Long amount;
}
