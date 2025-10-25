package com.daepamarket.daepa_market_backend.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductSearchDTO {
    private String keyword;     // 제목/내용 키워드
    private Long upperId;       // 상위 카테고리
    private Long middleId;      // 중위 카테고리
    private Long lowId;         // 하위 카테고리
    private Integer status;     // 상품 상태(예: 0=판매중)
    private String location;    // 지역 LIKE
    private Long minPrice;      // 최소 가격
    private Long maxPrice;
}
