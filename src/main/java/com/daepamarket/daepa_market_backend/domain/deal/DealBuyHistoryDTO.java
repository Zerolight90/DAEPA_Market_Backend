package com.daepamarket.daepa_market_backend.domain.deal;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DealBuyHistoryDTO {

    private Long dealId;          // d_idx
    private Long productId;       // pd_idx
    private String title;         // p.pd_title
    private Timestamp dealEndDate; // d.d_edate
    private Long agreedPrice;

    private Long dSell;
    private Long dBuy;
    private Long dStatus;
    private String dDeal;         // MEET / DELIVERY

    private Integer dvStatus;     // 배송 상태
    private Integer ckStatus;     // 검수 상태

    private Long buyerIdx;        // ✅ d.buyer.uIdx

    // 화면에서 버튼 보여줄 때 쓰고 싶으면
    private boolean showReviewBtn;
    private String statusText;

    public DealBuyHistoryDTO(
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
            Long buyerIdx
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
        this.buyerIdx = buyerIdx;

        // 상태 텍스트는 판매쪽이랑 비슷하게
        this.statusText = toStatusText(dSell, dBuy, dStatus);

        // 예시로: 배송까지 끝났으면 후기 버튼
        this.showReviewBtn = (dvStatus != null && dvStatus == 5);
    }

    private String toStatusText(Long dSell, Long dBuy, Long dStatus) {
        // 네가 쓰는 값에 맞춰서
        if (dStatus != null && dStatus == 1L) return "구매완료";
        if (dSell != null && dSell == 1L) return "결제완료";
        return "구매중";
    }
}
