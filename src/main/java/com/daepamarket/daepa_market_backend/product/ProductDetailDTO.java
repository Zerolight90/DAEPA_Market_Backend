package com.daepamarket.daepa_market_backend.product;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDTO {

    // 기본 상품
    private Long pdIdx;              // 상품 ID
    private String pdTitle;          // 상품 제목
    private Long pdPrice;            // 가격
    private String pdLocation;       // 거래 지역(DB 컬럼: pd_location)
    private String pdContent;        // 상품 설명
    private String pdThumb;          // 대표 썸네일 이미지
    private String pdCreate;         // 등록일 (ISO 문자열)

    // 이미지
    private List<String> images;     // ProductImageEntity.imageUrl

    // 판매자
    private Long sellerId;           // UserEntity.uIdx
    private String sellerName;       // UserEntity.uName or uNickname
    private String sellerAvatar;     // ✅ 프로필 이미지도 내려주면 프론트가 바로 씀
    // 🟢 추가: 판매자 매너(=신선도)
    // user 테이블의 u_manner 그대로 보내줄 거야
    private Double sellerManner;
    // 거래/상태
    private Integer pdStatus;        // ✅ 0=중고, 1=새상품
    private String dDeal;            // ✅ "DELIVERY" | "MEET"
    private String location;         // ✅ 프론트에서 meetLocation 으로도 쓰려고 한 번 더 준 값

    // 카테고리 이름
    private String upperName;        // ✅ 상위 카테고리명
    private String middleName;       // ✅ 중위 카테고리명
    private String lowName;          // ✅ 하위 카테고리명

    // 👇 추가
    private Long upperId;
    private Long middleId;
    private Long lowId;

    private Long dStatus;
}