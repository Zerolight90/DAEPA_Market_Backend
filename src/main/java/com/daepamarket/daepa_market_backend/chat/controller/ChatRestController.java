// ✅ src/main/java/com/daepamarket/daepa_market_backend/chat/controller/ChatRestController.java
package com.daepamarket.daepa_market_backend.chat.controller;

import com.daepamarket.daepa_market_backend.S3Service;
import com.daepamarket.daepa_market_backend.chat.service.ChatService;
import com.daepamarket.daepa_market_backend.chat.service.RoomService;
import com.daepamarket.daepa_market_backend.common.dto.ChatDto;
import com.daepamarket.daepa_market_backend.common.dto.ChatRoomHeaderDto;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
    private final S3Service s3Service; // ✅ 추가됨
    private final SimpMessagingTemplate broker;

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

    /** after(마지막 메시지ID) 초과만 ASC로 반환 → 폴링용 증분 조회 */
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

    /** ✅ 이미지 업로드 → S3 업로드 버전 */
    // ✅ ChatRestController.java 안의 upload 메서드를 아래 코드로 "그대로" 교체하세요.
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        // 1) 로그인 안전 처리
        Long userId;
        try {
            userId = jwtSupport.resolveUserIdFromCookie(request);
        } catch (Exception e) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "unauthorized");
            body.put("message", "로그인 정보를 해석하지 못했습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
        if (userId == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "unauthorized");
            body.put("message", "로그인이 필요합니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

        // 2) 파일 유효성
        if (file == null || file.isEmpty()) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "file_empty");
            return ResponseEntity.badRequest().body(body);
        }
        String ct = Optional.ofNullable(file.getContentType()).orElse("");
        if (!ct.startsWith("image/")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "only_image_allowed");
            body.put("contentType", ct);
            return ResponseEntity.badRequest().body(body);
        }

        try {
            var today = java.time.LocalDate.now();
            String folder = String.format("chat/%d/%02d", today.getYear(), today.getMonthValue());
            String s3Url = s3Service.uploadFile(file, folder);

            // ✅ 성공 응답도 Map.of 대신 null 안전한 Map 사용
            String safeCt = (file.getContentType() != null) ? file.getContentType() : "application/octet-stream";
            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("url", s3Url);
            ok.put("size", file.getSize());
            ok.put("contentType", safeCt);
            ok.put("uploaderId", userId);
            return ResponseEntity.ok(ok);

        } catch (Exception e) {
            // ✅ 에러 응답: Map.of 제거 (null 들어오면 NPE 나는 문제 해결)
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", "upload_failed");
            body.put("message", (e.getMessage() != null) ? e.getMessage() : e.getClass().getSimpleName());
            if (e.getCause() != null) {
                body.put("cause", e.getCause().toString());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }



    /** 문자열 → Long 변환 유틸 */
    private static Long parseLongOrNull(String s) {
        try { return (s == null || s.isBlank()) ? null : Long.valueOf(s.trim()); }
        catch (Exception e) { return null; }
    }

    // ✅ 상대방의 마지막 읽음 메시지ID 조회
    @GetMapping("/{roomId}/last-seen")
    public ResponseEntity<Map<String, Object>> lastSeen(
            @PathVariable Long roomId,
            @RequestParam("userId") Long userId,
            HttpServletRequest request
    ) {
        // (선택) 권한 체크: 로그인 여부만 간단히 확인
        Long me = jwtSupport.resolveUserIdFromCookie(request);
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }

        // DB에서 해당 (roomId, userId)의 last_seen_message_id 읽기
        Long v = messageMapper.selectLastSeen(roomId, userId);
        if (v == null) v = 0L;

        return ResponseEntity.ok(Map.of("lastSeenMessageId", v));
    }


    @GetMapping("/{roomId}/header")
    public ResponseEntity<ChatRoomHeaderDto> header(
            @PathVariable Long roomId,
            @RequestHeader(name = "x-user-id", required = false) Long userIdHeader,
            Principal principal,
            HttpServletRequest request
    ) {
        Long me =
                userIdHeader != null ? userIdHeader :
                        (principal != null ? parseLongOrNull(principal.getName()) : null);

        if (me == null) me = jwtSupport.resolveUserIdFromCookie(request);
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        ChatRoomHeaderDto dto = chatRoomMapper.selectRoomHeader(roomId, me);
        if (dto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 채팅방입니다.");

        return ResponseEntity.ok(dto);
    }

    /** ✅ 채팅방 나가기 (REST 폴백) */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<ChatDto.RoomEvent> leave(
            @PathVariable Long roomId,
            @RequestHeader(name = "x-user-id", required = false) Long userIdHeader,
            Principal principal,
            HttpServletRequest request
    ) {
        Long me =
                userIdHeader != null ? userIdHeader :
                        (principal != null ? parseLongOrNull(principal.getName()) : null);
        if (me == null) me = jwtSupport.resolveUserIdFromCookie(request);
        if (me == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

        // 참여자인지 확인(안전)
        if (!roomService.isParticipant(roomId, me)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 방의 참여자가 아닙니다.");
        }

        // 1) 내 참여 행 삭제
        int deleted = chatRoomMapper.deleteRead(roomId, me);
        if (deleted <= 0) {
            // 이미 나간 상태면 멱등 처리
        }

        // 2) LEAVE 이벤트 브로드캐스트(방 구독자용)
        ChatDto.RoomEvent ev = ChatDto.RoomEvent.builder()
                .type("LEAVE")
                .roomId(roomId)
                .actorId(me)
                .time(LocalDateTime.now())
                .build();
        broker.convertAndSend("/sub/chats/" + roomId, ev);

        // 3) 내 총 배지(TOTAL_BADGE) 재계산 후 푸시
        Integer total = chatRoomMapper.countTotalUnread(me);
        com.daepamarket.daepa_market_backend.common.dto.ChatBadgeDto totalBadge =
                com.daepamarket.daepa_market_backend.common.dto.ChatBadgeDto.builder()
                        .type("TOTAL_BADGE").userId(me)
                        .total(total == null ? 0 : total)
                        .time(LocalDateTime.now())
                        .build();
        broker.convertAndSend("/sub/users/" + me + "/chat-badge", totalBadge);

        return ResponseEntity.ok(ev);
    }



}
