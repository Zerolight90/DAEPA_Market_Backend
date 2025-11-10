package com.daepamarket.daepa_market_backend.oneonone;

import com.daepamarket.daepa_market_backend.jwt.CookieUtil;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/1on1")
@RequiredArgsConstructor
public class UserOneOnOneController {

    private final UserOneOnOneService oneOnOneService;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    /**
     * 1:1 문의 등록 (멀티파트)
     * form-data:
     *  - dto: { "status": 1, "title": "...", "content": "..." }
     *  - photo: (선택) 이미지 파일
     */
    @PostMapping(value = "/create-multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            HttpServletRequest request,
            @RequestPart("dto") UserOneOnOneCreateDTO dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        Long userId = resolveUserId(request);
        Long id = oneOnOneService.create(userId, dto, photo);
        return ResponseEntity.ok(id);
    }

    /**
     * 내 문의 목록 보기 (선택)
     */
    @GetMapping("/my")
    public ResponseEntity<?> myList(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        List<?> list = oneOnOneService.getMyInquiries(userId);
        return ResponseEntity.ok(list);
    }

    private Long resolveUserId(HttpServletRequest request) {
        String token = resolveAccessToken(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (jwtProvider.isExpired(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 만료");
        }
        return Long.valueOf(jwtProvider.getUid(token));
    }

    // ProductController 에 있던 거 그대로 가져온 버전
    private String resolveAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CookieUtil.ACCESS.equals(c.getName())) {
                    String v = c.getValue();
                    if (v != null && !v.isBlank()) return v;
                }
            }
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }
}
