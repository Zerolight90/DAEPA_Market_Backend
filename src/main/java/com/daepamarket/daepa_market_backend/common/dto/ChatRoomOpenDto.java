package com.daepamarket.daepa_market_backend.common.dto;

import lombok.*;

public class ChatRoomOpenDto {

    @Getter @Setter
    public static class OpenChatRoomReq {
        private Long productId;  // pdId
        private Long sellerId;   // 판매자 u_idx
    }

    @Builder @Getter
    public static class OpenChatRoomRes {
        private Long roomId;
        private boolean created;
        private String identifier;
    }
}
