// src/main/java/com/daepamarket/daepa_market_backend/chat/controller/ChatRestController.java
package com.daepamarket.daepa_market_backend.chat.controller;

import com.daepamarket.daepa_market_backend.chat.service.ChatService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.*;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatRestController {

    private final ChatMessageMapper messageMapper;
    private final ChatRoomMapper chatRoomMapper;
    private final RoomService roomService;
    private final JwtSupport jwtSupport;
    private final ChatService chatService;

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

    /** after(마지막 메시지ID) 초과만 ASC로 반환 → 폴링용 증분 조회(지금은 미사용) */
    @GetMapping("/{roomId}/messages-after")
    public List<ChatDto.MessageRes> messagesAfter(@PathVariable Long roomId,
                                                  @RequestParam Long after,
                                                  @RequestParam(defaultValue = "50") int size) {
        return messageMapper.findMessagesAfter(roomId, after, size);
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

    /** REST 폴백: 메시지 전송(WS 미연결 시 사용) */
    @PostMapping("/{roomId}/send")
    public ChatDto.MessageRes sendViaHttp(@PathVariable Long roomId,
                                          @RequestBody ChatDto.SendMessageReq req,
                                          Principal principal,
                                          HttpServletRequest http) {
        Long senderId = req.getSenderId();
        if (senderId == null && principal != null) senderId = parseLongOrNull(principal.getName());
        if (senderId == null) senderId = jwtSupport.resolveUserIdFromCookie(http);
        if (senderId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        return chatService.sendMessage(roomId, senderId, req.getText(), req.getImageUrl(), req.getTempId());
    }

    /** REST 폴백: 읽음 포인터 올리기(WS 미연결 시 사용) */
    @PostMapping("/{roomId}/read-up-to")
    public ChatDto.ReadEvent readUpTo(@PathVariable Long roomId,
                                      @RequestParam(required = false) Long upTo,
                                      @RequestBody(required = false) ChatDto.ReadEvent body,
                                      Principal principal,
                                      HttpServletRequest http) {
        Long readerId = (body != null && body.getReaderId() != null) ? body.getReaderId() : null;
        if (readerId == null && principal != null) readerId = parseLongOrNull(principal.getName());
        if (readerId == null) readerId = jwtSupport.resolveUserIdFromCookie(http);
        if (readerId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        Long targetUpTo = (upTo != null) ? upTo : (body != null ? body.getLastSeenMessageId() : null);
        Long applied = chatService.markRead(roomId, readerId, targetUpTo);

        return ChatDto.ReadEvent.builder()
                .type("READ")
                .roomId(roomId)
                .readerId(readerId)
                .lastSeenMessageId(applied)
                .time(LocalDateTime.now())
                .build();
    }

    /** ✅ 채팅 이미지 업로드(멀티파트) → { url } 반환 — 로컬 디스크 저장 */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) throws Exception {
        Long userId = jwtSupport.resolveUserIdFromCookie(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file is empty"));
        }
        // 확장자/타입 간단 체크(보안상 더 강화 가능)
        String ct = Optional.ofNullable(file.getContentType()).orElse("");
        if (!ct.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일만 허용됩니다."));
        }

        String originalName = StringUtils.cleanPath(
                Optional.ofNullable(file.getOriginalFilename()).orElse("image")
        );
        String ext = "";
        int idx = originalName.lastIndexOf('.');
        if (idx >= 0) ext = originalName.substring(idx);

        LocalDate today = LocalDate.now();
        Path base = Paths.get("uploads", "chat",
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()));
        Files.createDirectories(base);

        String saveName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dest = base.resolve(saveName);

        file.transferTo(dest.toFile());

        String url = "/files/chat/" + today.getYear()
                + "/" + String.format("%02d", today.getMonthValue())
                + "/" + saveName;

        Map<String, Object> body = new HashMap<>();
        body.put("url", url);
        body.put("name", saveName);
        body.put("size", file.getSize());
        body.put("contentType", file.getContentType());
        body.put("uploaderId", userId);
        return ResponseEntity.ok(body);
    }

    private static Long parseLongOrNull(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.valueOf(s.trim()); }
        catch (Exception e) { return null; }
    }
}
