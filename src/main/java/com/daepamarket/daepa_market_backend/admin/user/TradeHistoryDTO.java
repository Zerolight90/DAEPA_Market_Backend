package com.daepamarket.daepa_market_backend.admin.user;

import com.daepamarket.daepa_market_backend.domain.deal.DealEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeHistoryDTO {
    private Long dealId;
    private String tradeType;    // BUY or SELL
    private String title;        // 거래 방식 (DELIVERY / MEET)
    private String productName;  // 상품명
    private Long price;          // agreed_price
    private String date;         // d_edate
    private Long status;         // d_status

    public static TradeHistoryDTO fromEntity(DealEntity e, String type) {
        TradeHistoryDTO dto = new TradeHistoryDTO();
        dto.dealId = e.getDIdx();
        dto.tradeType = type;
        dto.title = e.getDDeal();
        dto.productName = (e.getProduct() != null && e.getProduct().getPdTitle() != null)
                        ? e.getProduct().getPdTitle() : "(상품명 없음)";
        dto.price = e.getAgreedPrice();
        dto.date = e.getDEdate() != null ? e.getDEdate().toString() : null;
        dto.status = e.getDStatus();
        return dto;
    }
}
