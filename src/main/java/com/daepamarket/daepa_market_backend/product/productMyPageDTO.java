// productMyPageDTO.java
package com.daepamarket.daepa_market_backend.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class productMyPageDTO {
    private Long pd_idx;
    private Long u_idx;
    private String pd_status;
    private String pd_create;
    private String pd_title;
    private int pd_price;
    private String pd_thumb;

    // 기존에 쓰던 거래상태
    private Long d_status;

    // ✅ 추가: 실제 판매완료 여부
    private Long d_sell;

    // 삭제/만료 정보
    private Boolean pd_del;
    private String pd_edate;
}
