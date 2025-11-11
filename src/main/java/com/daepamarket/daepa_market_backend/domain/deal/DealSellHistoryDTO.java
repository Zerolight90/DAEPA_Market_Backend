package com.daepamarket.daepa_market_backend.domain.deal;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class DealSellHistoryDTO {

    private Long dealId;
    private Long productId;
    private String title;
    private Timestamp dealEndDate;
    private Long agreedPrice;

    private Long dSell;
    private Long dBuy;
    private Long dStatus;
    private String dDeal;

    private Integer dvStatus;
    private Integer ckStatus;
    private Long sellerIdx2;

    private String orderId;
    private Long buyerIdx;
    private String buyerNickname;
    private String buyerPhone;


    // ✅ 여기 추가: 상품 썸네일
    private String productThumb;

    private boolean showSendBtn;
    private boolean showReviewBtn;
    private String statusText;

    public DealSellHistoryDTO(
            Long dealId,
            Long productId,
            String title,
            Timestamp dealEndDate,
            Long agreedPrice,
            Long dSell,
            Long dBuy,
            Long dStatus,
            String dDeal,
            Integer dvStatus,
            Integer ckStatus,
            Long sellerIdx2,
            String orderId,
            Long buyerIdx,
            String buyerNickname,
            String buyerPhone,
            String productThumb   // ✅ 맨 끝에 추가
    ) {
        this.dealId = dealId;
        this.productId = productId;
        this.title = title;
        this.dealEndDate = dealEndDate;
        this.agreedPrice = agreedPrice;
        this.dSell = dSell;
        this.dBuy = dBuy;
        this.dStatus = dStatus;
        this.dDeal = dDeal;
        this.dvStatus = dvStatus;
        this.ckStatus = ckStatus;
        this.sellerIdx2 = sellerIdx2;

        this.orderId = orderId;
        this.buyerIdx = buyerIdx;
        this.buyerNickname = buyerNickname;
        this.buyerPhone = buyerPhone;

        this.productThumb = productThumb;   // ✅

        this.statusText = toStatusText(dSell, dBuy, dStatus);

        boolean isSold = (dStatus != null && dStatus == 1L);
        boolean isDelivery = dDeal != null && dDeal.trim().equalsIgnoreCase("DELIVERY");
        this.showSendBtn = isSold && isDelivery && (dvStatus == null || dvStatus == 0);
        this.showReviewBtn = (dvStatus != null && dvStatus == 5);
    }

    private String toStatusText(Long dSell, Long dBuy, Long dStatus) {
        if (dStatus != null && dStatus == 1L) return "판매완료";
        if (dSell != null && dSell == 1L) return "결제완료";
        return "판매중";
    }
}
