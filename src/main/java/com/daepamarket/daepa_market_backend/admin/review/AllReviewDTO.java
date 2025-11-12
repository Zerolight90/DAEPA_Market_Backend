package com.daepamarket.daepa_market_backend.admin.review;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AllReviewDTO {
    private String id;
    private Long realId;
    private String product;
    private String buyer;
    private String seller;
    private Integer rating;
    private String comment;
    private LocalDateTime date;
    private String type;
    private Long buyerId;
    private Long sellerId;
    private Long writerId;
    private String writerName;
}
