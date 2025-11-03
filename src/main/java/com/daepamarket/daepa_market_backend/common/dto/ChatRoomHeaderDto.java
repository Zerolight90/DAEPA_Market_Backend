package com.daepamarket.daepa_market_backend.common.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomHeaderDto {
    private Long roomId;

    private Long productId;
    private String productTitle;
    private String productThumb;

    private Long sellerId;
    private Long buyerId;

    private String myRole;      // "판매자" | "구매자" | "참여자" (기존 표기 유지)
    private String saleStatus;  // "판매완료" | "판매중" (헤더용 간단 표기)
    private Long displayPrice;  // 거래 성사면 agreed_price, 아니면 pd_price
}
