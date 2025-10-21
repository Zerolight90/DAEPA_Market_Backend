package com.daepamarket.daepa_market_backend.domain.chatread;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "chat_reads",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_reads_room_reader", columnNames = {"ch_idx", "cread_reader"})
        }
)
public class ChatReadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cread_idx")
    private Long creadIdx;

    // 채팅방 ID
    @Column(name = "ch_idx", nullable = false)
    private Long roomId;

    // 사용자 (User.u_idx)
    @Column(name = "cread_reader", nullable = false)
    private Long readerId;

    // 마지막으로 읽은 메시지 ID
    @Column(name = "last_seen_message_id", nullable = false)
    private Long lastSeenMessageId;

    // 갱신 시각
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 생성 팩토리 메서드 */
    public static ChatReadEntity create(Long roomId, Long readerId, Long lastSeenMessageId) {
        return ChatReadEntity.builder()
                .roomId(roomId)
                .readerId(readerId)
                .lastSeenMessageId(lastSeenMessageId)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /** 읽은 위치 갱신 */
    public void updateLastSeen(Long newMessageId) {
        if (newMessageId != null && newMessageId > this.lastSeenMessageId) {
            this.lastSeenMessageId = newMessageId;
            this.updatedAt = LocalDateTime.now();
        }
    }
}

