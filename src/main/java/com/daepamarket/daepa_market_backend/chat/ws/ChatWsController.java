package com.daepamarket.daepa_market_backend.chat.ws;

import com.daepamarket.daepa_market_backend.chat.controller.JwtSupport;
import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.common.dto.ChatBadgeDto;
import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.mapper.ChatMessageMapper;
import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate broker;
    private final JwtSupport jwtSupport;

    private final ChatMessageMapper messageMapper;
    private final ChatRoomMapper chatRoomMapper;

    @PersistenceContext
    private EntityManager em;

    /** 💬 메시지 전송 */
    @MessageMapping("/chats/{roomId}/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            @Payload ChatDto.SendMessageReq req,
                            SimpMessageHeaderAccessor accessor,
                            Principal principal) {

        Long senderId = sanitizeId(req.getSenderId());
        if (senderId == null) senderId = parseLongSafe(accessor.getFirstNativeHeader("x-user-id"));
        if (senderId == null && principal != null) senderId = parseLongSafe(principal.getName());
        if (senderId == null) senderId = jwtSupport.resolveUserIdFromHeaderOrCookie(accessor);
        if (senderId == null) return;

        req.setRoomId(roomId);
        req.setSenderId(senderId);

        ChatDto.MessageRes res = chatService.sendMessage(roomId, senderId, req.getText(), req.getImageUrl(), req.getTempId());
        broker.convertAndSend("/sub/chats/" + roomId, res);

        broadcastBadgesAfterNewMessage(roomId, senderId);
    }

    /** 👁 읽음 처리 */
    @MessageMapping("/chats/{roomId}/read")
    public void markRead(@DestinationVariable Long roomId,
                         @Payload ChatDto.ReadEvent readEvent,
                         SimpMessageHeaderAccessor accessor,
                         Principal principal) {

        Long readerId = sanitizeId(readEvent.getReaderId());
        if (readerId == null) readerId = parseLongSafe(accessor.getFirstNativeHeader("x-user-id"));
        if (readerId == null && principal != null) readerId = parseLongSafe(principal.getName());
        if (readerId == null) readerId = jwtSupport.resolveUserIdFromHeaderOrCookie(accessor);
        if (readerId == null) return;

        Long appliedUpTo = chatService.markRead(roomId, readerId, readEvent.getLastSeenMessageId());

        ChatDto.ReadEvent ack = ChatDto.ReadEvent.builder()
                .type("READ")
                .roomId(roomId)
                .readerId(readerId)
                .lastSeenMessageId(appliedUpTo)  // ✅ 이제 항상 숫자 보장됨
                .time(LocalDateTime.now())
                .build();

        broker.convertAndSend("/sub/chats/" + roomId, ack);

        broadcastBadgesAfterRead(roomId, readerId);
    }

    /* =====================================================
       🔔 배지(안읽음 수) 브로드캐스트
       ===================================================== */

    private void broadcastBadgesAfterNewMessage(Long roomId, Long senderId) {
        List<Long> participants = findParticipants(roomId);
        for (Long uid : participants) {
            if (uid == null || uid.equals(senderId)) {
                continue;
            }

            int roomUnread = safeInt(messageMapper.countUnread(roomId, uid));
            ChatBadgeDto roomBadge = ChatBadgeDto.builder()
                    .type("ROOM_BADGE")
                    .userId(uid)
                    .roomId(roomId)
                    .unread(roomUnread)
                    .time(LocalDateTime.now())
                    .build();
            broker.convertAndSend("/sub/users/" + uid + "/chat-badge", roomBadge);

            Integer total = chatRoomMapper.countTotalUnread(uid);
            ChatBadgeDto totalBadge = ChatBadgeDto.builder()
                    .type("TOTAL_BADGE")
                    .userId(uid)
                    .total(total == null ? 0 : total)
                    .time(LocalDateTime.now())
                    .build();
            broker.convertAndSend("/sub/users/" + uid + "/chat-badge", totalBadge);
        }
    }

    private void broadcastBadgesAfterRead(Long roomId, Long readerId) {
        int roomUnread = safeInt(messageMapper.countUnread(roomId, readerId));
        ChatBadgeDto roomBadge = ChatBadgeDto.builder()
                .type("ROOM_BADGE")
                .userId(readerId)
                .roomId(roomId)
                .unread(roomUnread)
                .time(LocalDateTime.now())
                .build();
        broker.convertAndSend("/sub/users/" + readerId + "/chat-badge", roomBadge);

        Integer total = chatRoomMapper.countTotalUnread(readerId);
        ChatBadgeDto totalBadge = ChatBadgeDto.builder()
                .type("TOTAL_BADGE")
                .userId(readerId)
                .total(total == null ? 0 : total)
                .time(LocalDateTime.now())
                .build();
        broker.convertAndSend("/sub/users/" + readerId + "/chat-badge", totalBadge);
    }

    /* =====================================================
       🧩 유틸 메서드들
       ===================================================== */

    @SuppressWarnings("unchecked")
    private List<Long> findParticipants(Long roomId) {
        List<?> resultList = em.createNativeQuery(
                        "SELECT cread_reader " +
                                "FROM chat_reads " +
                                "WHERE ch_idx = :roomId"
                )
                .setParameter("roomId", roomId)
                .getResultList();

        List<Long> participants = new ArrayList<>();
        Iterator<?> iterator = resultList.iterator();
        while (iterator.hasNext()) {
            Object obj = iterator.next();
            if (obj instanceof Number) {
                participants.add(((Number) obj).longValue());
            }
        }
        return participants;
    }

    private static Long sanitizeId(Long v) {
        if (v == null) return null;
        return v;
    }

    private static Long parseLongSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static int safeInt(Integer v) {
        if (v == null) return 0;
        return v;
    }
}
