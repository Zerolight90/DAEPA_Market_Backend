package com.daepamarket.daepa_market_backend.chat.controller;


import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
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

    @GetMapping("/{roomId}/messages")
    public List<ChatDto.MessageRes> messages(@PathVariable Long roomId,
                                             @RequestParam(required=false) Long before,
                                             @RequestParam(defaultValue="30") int size) {
        return messageMapper.findMessages(roomId, before, size);
    }

    @PostMapping("/{roomId}/read")
    public void markRead(@PathVariable Long roomId, @RequestParam Long userId) {
        chatService.markReadToLatest(roomId, userId);
    }
}





