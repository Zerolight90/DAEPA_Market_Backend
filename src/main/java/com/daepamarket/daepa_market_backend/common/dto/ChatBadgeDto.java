package com.daepamarket.daepa_market_backend.common.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatBadgeDto {
    private String type;        // "ROOM_BADGE" | "TOTAL_BADGE"
    private Long userId;        // 대상 유저
    private Long roomId;        // ROOM_BADGE일 때만
    private Integer unread;     // ROOM_BADGE 안읽음 수
    private Integer total;      // TOTAL_BADGE 합계
    private LocalDateTime time; // 서버 시간
}
