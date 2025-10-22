package com.daepamarket.daepa_market_backend.chat.controller;

import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatQueryController {
    private final ChatMessageMapper messageMapper;
    private final ChatService chatService;

    /**
     * 메시지 목록: roomId가 "[9021]" 처럼 들어와도 안전하게 숫자만 추출
     * before 커서도 "null" / "undefined" / "" 를 안전 처리
     */
    @GetMapping("/{roomId}/messages")
    public List<ChatDto.MessageRes> messages(@PathVariable("roomId") String roomIdParam,
                                             @RequestParam(required = false) String before,
                                             @RequestParam(defaultValue = "30") int size) {
        Long roomId  = parseLongFromAny(roomIdParam);   // "[9021]" -> 9021
        Long beforeL = parseLongNullable(before);       // "null"/"undefined"/"" -> null
        return messageMapper.findMessages(roomId, beforeL, size);
    }

    /**
     * 읽음 표시: roomId, userId 모두 안전 파싱
     */
    @PostMapping("/{roomId}/read")
    public void markRead(@PathVariable("roomId") String roomIdParam,
                         @RequestParam("userId") String userIdParam) {
        Long roomId = parseLongFromAny(roomIdParam);
        Long userId = parseLongFromAny(userIdParam);
        chatService.markReadToLatest(roomId, userId);
    }

    // ----------------- helpers -----------------

    /** "[9021]" "%5B9021%5D" "9021" 등에서 숫자만 꺼내 Long */
    private Long parseLongFromAny(String raw) {
        if (raw == null) throw new IllegalArgumentException("id is null");
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) throw new IllegalArgumentException("id has no digits: " + raw);
        return Long.valueOf(digits);
    }

    /** "null" / "undefined" / "" 를 null 로, 나머지는 숫자만 추출 */
    private Long parseLongNullable(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase();
        if (s.isEmpty() || "null".equals(s) || "undefined".equals(s)) return null;
        return parseLongFromAny(raw);
    }
}
