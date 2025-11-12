// src/main/java/com/daepamarket/daepa_market_backend/review/dto/MyReviewRow.java
package com.daepamarket.daepa_market_backend.review.dto;

import java.time.LocalDateTime;

public class MyReviewRow {
    private Long reIdx;
    private String reType;
    private Integer reStar;
    private String reContent;
    private LocalDateTime reUpdate;

    private Long dIdx;
    private String productTitle;
    private String productThumb;
    private String writerNickname;

    // ✅ JPQL constructor expression 과 100% 일치하는 생성자
    public MyReviewRow(
            Long reIdx,
            String reType,
            Integer reStar,
            String reContent,
            LocalDateTime reUpdate,
            Long dIdx,
            String productTitle,
            String productThumb,
            String writerNickname
    ) {
        this.reIdx = reIdx;
        this.reType = reType;
        this.reStar = reStar;
        this.reContent = reContent;
        this.reUpdate = reUpdate;
        this.dIdx = dIdx;
        this.productTitle = productTitle;
        this.productThumb = productThumb;
        this.writerNickname = writerNickname;
    }

    // ===== getters =====
    public Long getReIdx() { return reIdx; }
    public String getReType() { return reType; }
    public Integer getReStar() { return reStar; }
    public String getReContent() { return reContent; }
    public LocalDateTime getReUpdate() { return reUpdate; }

    public Long getDIdx() { return dIdx; }
    public String getProductTitle() { return productTitle; }
    public String getProductThumb() { return productThumb; }
    public String getWriterNickname() { return writerNickname; }
}
