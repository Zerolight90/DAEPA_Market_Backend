package com.daepamarket.daepa_market_backend.domain.deal;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DealBuyHistoryDTO {

    private Long dealId;          // d_idx
    private Long productId;       // pd_idx
    private String title;         // 상품명
    private Timestamp dealEndDate; // d_edate (결제일시)
    private Long agreedPrice;

    private Long dSell;
    private Long dBuy;
    private Long dStatus;
    private String dDeal;         // MEET / DELIVERY

    private Integer dvStatus;     // delivery.dv_status
    private Integer ckStatus;     // check.ck_status

    // 내가 산 거니까 "판매자" 정보가 필요함
    private Long sellerIdx;       // d.seller.uIdx
    private String sellerNickname;
    private String sellerPhone;

    // 거래번호
    private String orderId;

    // 썸네일
    private String productThumb;

    // 프론트에서 단계 표시용
    private boolean showReviewBtn;
    private String statusText;

    /**
     * JPQL new ...() 에서 그대로 받을 수 있게 생성자 순서 명확히 함
     *
     * new com.daepamarket.daepa_market_backend.domain.deal.DealBuyHistoryDTO(
     *     d.dIdx,
     *     p.pdIdx,
     *     p.pdTitle,
     *     d.dEdate,
     *     d.agreedPrice,
     *     d.dSell,
     *     d.dBuy,
     *     d.dStatus,
     *     d.dDeal,
     *     dv.dvStatus,
     *     ck.ckStatus,
     *     d.seller.uIdx,
     *     d.seller.unickname,
     *     d.seller.uphone,
     *     d.orderId,
     *     p.pdThumb
     * )
     */
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
            Long sellerIdx,
            String sellerNickname,
            String sellerPhone,
            String orderId,
            String productThumb
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

        this.sellerIdx = sellerIdx;
        this.sellerNickname = sellerNickname;
        this.sellerPhone = sellerPhone;

        this.orderId = orderId;
        this.productThumb = productThumb;

        // 상태 텍스트 만들기
        this.statusText = toStatusText(dSell, dBuy, dStatus);

        // 내가 산 입장에서는 배송을 “보낸다” 버튼은 없음
        // 리뷰는 배송이 끝났거나 거래가 끝났을 때만 노출
        boolean tradeFinished = (dStatus != null && dStatus == 1L);
        boolean deliveryFinished = (dvStatus != null && dvStatus == 5);
        this.showReviewBtn = tradeFinished || deliveryFinished;
    }

    private String toStatusText(Long dSell, Long dBuy, Long dStatus) {
        if (dStatus != null && dStatus == 1L) return "구매완료";
        if (dBuy != null && dBuy == 1L) return "결제완료";
        return "진행중";
    }
}
