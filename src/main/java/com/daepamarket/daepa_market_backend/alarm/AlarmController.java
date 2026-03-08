package com.daepamarket.daepa_market_backend.alarm;

import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.daepamarket.daepa_market_backend.jwt.CookieUtil;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;

    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<Void> deleteNotification(
            HttpServletRequest request,
            @PathVariable Long productId) {

        String token = cookieUtil.getAccessTokenFromCookie(request);
        if (token == null || token.isBlank() || jwtProvider.isExpired(token)) {
            throw new SecurityException("유효하지 않은 토큰입니다."); // (또는 리턴값에 맞게 변경)
        }
        Long uIdx = Long.valueOf(jwtProvider.getUid(token));

        alarmService.deleteNotification(uIdx, productId);
        return ResponseEntity.ok().build();
    }
}