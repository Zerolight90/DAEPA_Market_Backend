package com.daepamarket.daepa_market_backend.admin.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AllReviewDTO {
    private String id;  // Long → String으로 변경
    private Long realId;
    private String product;
    private String buyer;
    private String seller;
    private Long rating;
    private String comment;
    private LocalDateTime date;
    private String type; // BUY / SELL
}
