package com.daepamarket.daepa_market_backend.common.dto;

import com.daepamarket.daepa_market_backend.domain.chat.ChatMessageEntity;
import lombok.*;

import java.time.LocalDateTime;

public class ChatDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SendMessageReq {
        private Long roomId;     // ch_idx
        private Long senderId;   // 보낸 사람
        private String text;     // 텍스트(선택)
        private String imageUrl; // 이미지(선택)
        private String tempId;   // 클라 낙관적 업데이트 매칭용(선택)
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MessageRes {
        private String type;       // "TEXT" | "IMAGE" | "SYSTEM"
        private Long messageId;    // cm_idx
        private Long roomId;       // ch_idx
        private Long senderId;
        private String content;
        private String imageUrl;
        private LocalDateTime time;
        private String tempId;     // (옵션) 클라가 보냈던 임시 id
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReadEvent {
        private String type;              // "READ"
        private Long roomId;              // ch_idx
        private Long readerId;            // 읽은 사용자
        private Long lastSeenMessageId;   // "여기까지 읽음" (옵션)
        private LocalDateTime time;       // 서버 세팅
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
