package com.daepamarket.daepa_market_backend.favorite;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FavoriteItemDTO {
    private Long id;        // 상품 PK (pdIdx)
    private String title;   // 상품명
    private Long price;     // 가격
    private String imageUrl; // 썸네일(없으면 null)
}