package com.daepamarket.daepa_market_backend.chat.controller;

import com.daepamarket.daepa_market_backend.chat.service.RoomService;
import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomListDto;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomReq;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomRes;
import com.daepamarket.daepa_market_backend.mapper.ChatMessageMapper;
import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatRestController {

    private final ChatMessageMapper messageMapper;
    private final ChatRoomMapper chatRoomMapper;
    private final RoomService roomService;   // 방 생성/재사용(JPA+MyBatis 보조)
    private final JwtSupport jwtSupport;

    /** 내 채팅방 목록 */
    @GetMapping("/my-rooms")
    public List<ChatRoomListDto> myRooms(
            @RequestParam(required = false) Long userId,
            @RequestHeader(name = "x-user-id", required = false) Long userIdHeader,
            Principal principal,
            HttpServletRequest request
    ) {
        Long effectiveUserId =
                userId != null ? userId :
                        userIdHeader != null ? userIdHeader :
                                (principal != null ? parseLongOrNull(principal.getName()) : null);

        if (effectiveUserId == null) {
            effectiveUserId = jwtSupport.resolveUserIdFromCookie(request);
        }
        if (effectiveUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return chatRoomMapper.listRooms(effectiveUserId);
    }

    /** 채팅 메시지 히스토리 (오래된 → 최신, ASC) */
    @GetMapping("/{roomId}/messages")
    public List<ChatDto.MessageRes> messages(@PathVariable Long roomId,
                                             @RequestParam(required = false) Long before,
                                             @RequestParam(defaultValue = "30") int size) {
        return messageMapper.findMessages(roomId, before, size);
    }

    /** 채팅방 생성/재사용 */
    @PostMapping("/open")
    public OpenChatRoomRes open(@RequestBody OpenChatRoomReq req,
                                Principal principal,
                                HttpServletRequest http) {
        Long buyerId = (principal != null) ? parseLongOrNull(principal.getName()) : null;
        if (buyerId == null) buyerId = jwtSupport.resolveUserIdFromCookie(http);
        if (buyerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        return roomService.openOrGetRoom(req, buyerId);
    }

    private static Long parseLongOrNull(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.valueOf(s.trim()); }
        catch (Exception e) { return null; }
    }
}
