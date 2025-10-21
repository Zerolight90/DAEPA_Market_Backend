package com.daepamarket.daepa_market_backend.domain.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    public enum MessageType { TEXT, IMAGE, SYSTEM }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cm_idx")
    private Long cmIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ch_idx", nullable = false)
    private ChatRoomEntity room;

    @Column(name = "cm_content", length = 1000)
    private String cmContent;

    /** ✅ DB 컬럼과 통일 (로컬 이미지 경로 → 추후 S3 전환 가능) */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "cm_date", nullable = false)
    private LocalDateTime cmDate;

    @Column(name = "cm_writer", nullable = false)
    private String cmWriter;

    @Column(name = "sender_id")
    private Long senderId;
}
