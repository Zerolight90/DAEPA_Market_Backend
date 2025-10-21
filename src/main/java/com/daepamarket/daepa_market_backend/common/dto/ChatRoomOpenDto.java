package com.daepamarket.daepa_market_backend.common.dto;

import lombok.*;

public class ChatRoomOpenDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OpenChatRoomReq {
        private Long buyerId;     // User.u_idx
        private Long sellerId;    // User.u_idx
        private Long productId;   // Product.pd_idx
        private Long dealId;      // Deal.d_idx (DB가 NOT NULL이라 필수)
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OpenChatRoomRes {
        private Long roomId;      // chat_rooms.ch_idx
        private boolean created;  // 새로 만들었는지/재사용했는지
        private String identifier;
    }
}
