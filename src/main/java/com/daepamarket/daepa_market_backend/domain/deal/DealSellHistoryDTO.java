package com.daepamarket.daepa_market_backend.domain.deal;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DealSellHistoryDTO {

    private Long dealId;       // d_idx
    private Long productId;    // pd_idx

    private String title;      // pd_title
    private LocalDateTime productEndDate; // pd_edate  ← 엔티티가 LocalDate니까 이걸로

    private Long agreedPrice;   // Long으로

    private Long dSell;
    private Long dBuy;
    private Long dStatus;

    private String dDeal;

    private String statusText;

    public DealSellHistoryDTO(
            Long dealId,
            Long productId,
            String title,
            LocalDateTime productEndDate,
            Long agreedPrice,
            Long dSell,
            Long dBuy,
            Long dStatus,
            String dDeal
    ) {
        this.dealId = dealId;
        this.productId = productId;
        this.title = title;
        this.productEndDate = productEndDate;
        this.agreedPrice = agreedPrice;
        this.dSell = dSell;
        this.dBuy = dBuy;
        this.dStatus = dStatus;
        this.dDeal = dDeal;
        this.statusText = toStatusText(dSell, dBuy, dStatus);
    }

    private String toStatusText(Long dSell, Long dBuy, Long dStatus) {
        if (dStatus != null && dStatus == 1L) return "판매완료";
        if (dSell != null && dSell == 1L) return "결제완료";
        return "판매중";
    }
}