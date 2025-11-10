package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
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
    private Long d_status;
}


