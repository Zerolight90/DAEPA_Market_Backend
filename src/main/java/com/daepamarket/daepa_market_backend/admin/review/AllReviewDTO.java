package com.daepamarket.daepa_market_backend.admin.review;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AllReviewDTO {
    private String id;          // "S-1" / "B-10"
    private Long realId;        // 실제 PK
    private String product;
    private String buyer;
    private String seller;
    private Integer rating;     // ★ 여기 Long → Integer 로
    private String comment;
    private LocalDateTime date;
    private String type;        // "BUYER" / "SELLER"
}
