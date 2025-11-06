package com.daepamarket.daepa_market_backend.admin.review;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SaleReviewDTO {
    private Long reviewId;
    private String reviewerName;
    private Long  rating;
    private String content;
    private LocalDateTime date;
    private String productName;
}
