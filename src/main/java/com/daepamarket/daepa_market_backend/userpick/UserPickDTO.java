package com.daepamarket.daepa_market_backend.userpick;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPickDTO {
    private Long upIdx;
    private String upperCategory;
    private String middleCategory;
    private String lowCategory;
    private int minPrice;
    private int maxPrice;
}
