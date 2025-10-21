package com.daepamarket.daepa_market_backend.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductListDTO {
    private Long   id;         // pdIdx
    private String title;      // pdTitle
    private Long   price;      // pdPrice
    private String thumbnail;  // pdThumb
    private String location;   // pdLocation
    private String createdAt;  // ISO 문자열 (pdCreate)
}

