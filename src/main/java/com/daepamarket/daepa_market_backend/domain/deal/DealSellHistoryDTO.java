package com.daepamarket.daepa_market_backend.domain.deal;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class DealSellHistoryDTO {

    private Long dealId;       // d_idx
    private Long productId;    // pd_idx

    private String title;      // pd_title
    private Timestamp dealEndDate; // d_edate

    private Long agreedPrice;

    private Long dSell;
    private Long dBuy;
    private Long dStatus;

    private String dDeal;      // "MEET" / "DELIVERY"

    private Integer dvStatus;    // delivery.dv_status
    private Integer ckStatus;    // check.ck_status

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
            String dDeal,        // 문자열 그대로 받음
            Integer dvStatus,
            Integer ckStatus
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

        this.statusText = toStatusText(dSell, dBuy, dStatus);

        // 판매완료 + 택배거래 + 아직 안 눌렀다
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
