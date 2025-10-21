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
    private String lastMessage;
    private LocalDateTime lastAt; // cm_date가 DATETIME → MyBatis가 자동 변환
    private Integer unread;
}
