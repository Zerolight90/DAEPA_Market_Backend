package com.daepamarket.daepa_market_backend.product;

import com.daepamarket.daepa_market_backend.domain.product.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductCardDTO {
    private Long id;          // pdIdx
    private String title;
    private long price;
    private String thumbnail; // 썸네일 URL만 사용
    private Integer status;   // 0/1 등

    public static ProductCardDTO from(ProductEntity p) {
        // ✅ LAZY 컬렉션인 p.getImages() 절대 건드리지 말 것
        String thumb = (p.getPdThumb() != null && !p.getPdThumb().isBlank())
                ? p.getPdThumb()
                : "/no-image.png";

        return new ProductCardDTO(
                p.getPdIdx(),
                p.getPdTitle(),
                p.getPdPrice(),
                thumb,
                p.getPdStatus()
        );
    }
}
