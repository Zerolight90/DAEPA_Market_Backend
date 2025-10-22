package com.daepamarket.daepa_market_backend.product;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDTO {

    private Long pdIdx;              // 상품 ID
    private String pdTitle;          // 상품 제목
    private Long pdPrice;            // 가격
    private String pdLocation;       // 거래 지역
    private String pdContent;        // 상품 설명
    private String pdThumb;          // 대표 썸네일 이미지 (없으면 images[0])
    private String pdCreate;         // 등록일 (ISO 문자열)

    // ✅ 이미지 URL 리스트 (ProductImageEntity.imageUrl)
    private List<String> images;

    // ✅ 판매자 정보
    private Long sellerId;           // 판매자 ID (UserEntity.uIdx)
    private String sellerName;       // 판매자 이름 (UserEntity.uName)
}
