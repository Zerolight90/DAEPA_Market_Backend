package com.daepamarket.daepa_market_backend.review.dto;

import java.time.LocalDateTime;

public class MyReviewRow {
    private Long reIdx;
    private Long dIdx;
    private String productTitle;
    private String productThumb;
    private String writerNickname;
    private Integer reStar;
    private String reContent;
    private LocalDateTime reCreate;
    private LocalDateTime reUpdate;  // ✅ 추가
    private String reType;

    public MyReviewRow(Long reIdx,
                       Long dIdx,
                       String productTitle,
                       String productThumb,
                       String writerNickname,
                       Integer reStar,
                       String reContent,
                       LocalDateTime reCreate,
                       LocalDateTime reUpdate,   // ✅ 생성자 추가
                       String reType) {
        this.reIdx = reIdx;
        this.dIdx = dIdx;
        this.productTitle = productTitle;
        this.productThumb = productThumb;
        this.writerNickname = writerNickname;
        this.reStar = reStar;
        this.reContent = reContent;
        this.reCreate = reCreate;
        this.reUpdate = reUpdate;
        this.reType = reType;
    }

    public Long getReIdx() { return reIdx; }
    public Long getDIdx() { return dIdx; }
    public String getProductTitle() { return productTitle; }
    public String getProductThumb() { return productThumb; }
    public String getWriterNickname() { return writerNickname; }
    public Integer getReStar() { return reStar; }
    public String getReContent() { return reContent; }
    public LocalDateTime getReCreate() { return reCreate; }
    public LocalDateTime getReUpdate() { return reUpdate; }  // ✅ Getter 추가
    public String getReType() { return reType; }
}
