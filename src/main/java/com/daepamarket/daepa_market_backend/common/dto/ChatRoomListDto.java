package com.daepamarket.daepa_market_backend.common.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomListDto {
    private Long roomId;
    private Long counterpartyId;
    private String counterpartyName;
    private String myRole;
    private String statusBadge;
    private String productTitle;
    private String productThumb;

    // ⬇️ 추가 (표시 가격)
    private Long displayPrice;

    private String lastMessage;
    private LocalDateTime lastAt;
    private Integer unread;
}
