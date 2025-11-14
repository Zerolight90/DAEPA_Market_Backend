package com.daepamarket.daepa_market_backend.pay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentConfirmDto {
    private String paymentKey;
    private String orderId;
    private Long amount;
}
