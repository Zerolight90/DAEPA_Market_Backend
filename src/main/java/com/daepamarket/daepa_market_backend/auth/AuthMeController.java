package com.daepamarket.daepa_market_backend.auth;

import com.daepamarket.daepa_market_backend.chat.controller.JwtSupport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthMeController {

    private final JwtSupport jwtSupport;

    @GetMapping("/me")
    public ResponseEntity<MeRes> me(HttpServletRequest request) {
        Long uid = jwtSupport.resolveUserIdFromCookie(request);
        if (uid == null) {
            return ResponseEntity.ok(new MeRes(null, null, false));
        }
        return ResponseEntity.ok(new MeRes(uid, uid, true));
    }

    @Data
    @AllArgsConstructor
    static class MeRes {
        private Long userId; // 기존 코드를 위해 유지
        private Long u_idx;  // 새로운 요구사항을 위해 추가
        private boolean authenticated;
    }
}
