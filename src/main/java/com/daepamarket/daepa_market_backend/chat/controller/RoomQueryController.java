package com.daepamarket.daepa_market_backend.chat.controller;

import com.daepamarket.daepa_market_backend.common.dto.ChatRoomListDto;
import com.daepamarket.daepa_market_backend.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoomQueryController {

    private final ChatRoomMapper chatRoomMapper;

    @GetMapping("/my-rooms")
    public List<ChatRoomListDto> myRooms(
            @RequestParam(required = false) Long userId,
            @RequestHeader(name = "x-user-id", required = false) Long userIdHeader,
            Principal principal
    ) {
        Long effectiveUserId =
                userId != null ? userId :
                        userIdHeader != null ? userIdHeader :
                                (principal != null ? parseLongOrNull(principal.getName()) : null);

        if (effectiveUserId == null) {
            throw new IllegalArgumentException("userId가 필요합니다(쿼리스트링 ?userId=, 혹은 x-user-id 헤더/로그인).");
        }
        return chatRoomMapper.listRooms(effectiveUserId);
    }

    private static Long parseLongOrNull(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.valueOf(s.trim()); }
        catch (Exception e) { return null; }
    }
}
