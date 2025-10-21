package com.daepamarket.daepa_market_backend.common.dto;

import com.daepamarket.daepa_market_backend.domain.chat.ChatMessageEntity;
import lombok.*;
import java.time.LocalDateTime;

public class ChatDto {
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SendMessageReq {
        private Long roomId;
        private Long senderId;
        private String text;
        private String imageUrl;
        private String tempId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MessageRes {
        private String type;     // "TEXT" | "IMAGE"
        private Long messageId;  // cm_idx
        private Long roomId;     // ch_idx
        private Long senderId;
        private String content;
        private String imageUrl;
        private LocalDateTime time;
        private String tempId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReadEvent {
        private String type;     // "READ"
        private Long roomId;
        private Long readerId;
        private Long lastSeenMessageId;
        private LocalDateTime time;
    }

    public static MessageRes fromEntity(ChatMessageEntity e) {
        return MessageRes.builder()
                .type(e.getImageUrl() != null ? "IMAGE" : "TEXT")
                .messageId(e.getCmIdx())
                .roomId(e.getRoom().getChIdx())
                .senderId(e.getSenderId())
                .content(e.getCmContent())
                .imageUrl(e.getImageUrl())
                .time(e.getCmDate())
                .build();
    }
}
