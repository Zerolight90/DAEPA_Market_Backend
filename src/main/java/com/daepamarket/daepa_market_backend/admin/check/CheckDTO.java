package com.daepamarket.daepa_market_backend.admin.check;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckDTO {
    private Long ckIdx;
    private Long dIdx;
    private String productName;
    private String sellerName;
    private String tradeType;
    private Integer ckStatus;  // 0: 검수중, 1: 완료
    private Integer ckResult;  // 0: 불합격, 1: 합격
    private Integer dvStatus;  // 배송 상태: 0: 배송전, 1: 배송중, 2: 검수배송완료, 3: 검수 후 배송, 4: 반품, 5: 배송완료
}
