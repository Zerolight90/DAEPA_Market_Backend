package com.daepamarket.daepa_market_backend.userpick;

import com.daepamarket.daepa_market_backend.domain.userpick.UserPickEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserPickCreateRequestDto {
    private Long upIdx;
    private String upperCategory;
    private String middleCategory;
    private String lowCategory;
    private Integer minPrice;
    private Integer maxPrice;

    // Entity를 DTO로 변환하는 생성자
    public UserPickCreateRequestDto(UserPickEntity entity) {
        this.upIdx = entity.getUpIdx();
        this.upperCategory = entity.getCtUpper().getUpperCt(); // Entity의 필드명에 맞게 수정
        this.middleCategory = entity.getCtMiddle().getMiddleCt();
        this.lowCategory = entity.getCtLow().getLowCt();
        this.minPrice = entity.getMinPrice();
        this.maxPrice = entity.getMaxPrice();
    }
}