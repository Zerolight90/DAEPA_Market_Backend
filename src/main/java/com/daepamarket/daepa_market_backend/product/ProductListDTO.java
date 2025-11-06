package com.daepamarket.daepa_market_backend.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListDTO {
    private Long   pdIdx;         // pdIdx
    private String pdTitle;      // pdTitle
    private Long   pdPrice;      // pdPrice
    private String pdThumb;  // pdThumb
    private String pdLocation;   // pdLocation
    private String pdCreate;  // ISO 문자열 (pdCreate)
    private Long dStatus; // 0=판매중,1=판매완료
}

