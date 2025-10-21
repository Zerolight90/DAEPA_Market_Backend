package com.daepamarket.daepa_market_backend.product;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;


import java.util.List;

/** 상품 등록 요청 DTO (Enum 없이 DB컬럼과 직접 매핑) */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCreateDTO {

    // 카테고리 (상 → 중 → 하)
    @NotNull(message = "상위 카테고리를 선택하세요.")
    private Long upperId;

    @NotNull(message = "중위 카테고리를 선택하세요.")
    private Long middleId;

    @NotNull(message = "하위 카테고리를 선택하세요.")
    private Long lowId;

    // 상품 기본 정보
    @NotBlank(message = "상품명을 입력하세요.")
    @Size(max = 100, message = "상품명은 100자 이내로 입력하세요.")
    private String title;          // Product.pd_title

    @NotNull(message = "가격을 입력하세요.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private Long price;            // Product.pd_price

    @Size(max = 1000, message = "상세 설명은 1000자 이내로 입력하세요.")
    private String content;        // Product.pd_content

    @Size(max = 20, message = "지역은 20자 이내로 입력하세요.")
    private String location;       // Product.pd_location (선택)

    // 상태 / 거래방식 (DB 컬럼값 그대로 사용)
    @NotNull(message = "상품 상태를 선택하세요.")
    @Min(0) @Max(1)
    private Integer pdStatus;      // Product.pd_status (0=중고, 1=새상품)

    @NotBlank(message = "거래 방식을 선택하세요.")
    @Pattern(regexp = "DELIVERY|MEET", message = "거래 방식은 DELIVERY 또는 MEET 만 허용됩니다.")
    @JsonProperty("dDeal")
    @JsonAlias({"deal","tradeMethod"})   // ← 혹시 프론트에서 다른 이름으로 올 때도 매핑
    private String dDeal;       // Deal.d_deal

    // 이미지 URL (0~10개)
    @Size(max = 10, message = "이미지는 최대 10개까지 업로드 가능합니다.")
    private List<@NotBlank String> imageUrls;
}