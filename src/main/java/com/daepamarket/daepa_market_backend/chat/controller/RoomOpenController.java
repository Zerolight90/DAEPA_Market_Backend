package com.daepamarket.daepa_market_backend.chat.controller;

import com.daepamarket.daepa_market_backend.chat.service.RoomService;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomReq;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomOpenDto.OpenChatRoomRes;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class RoomOpenController {

    private final RoomService roomService;

    @PostMapping("/open")
    public OpenChatRoomRes open(@RequestBody OpenChatRoomReq req) {
        return roomService.openOrGetRoom(req);
    }
}
