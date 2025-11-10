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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;
    private final JwtProvider jwtProvider;

    @DeleteMapping("/delete/{productId}")
    public ResponseEntity<Void> deleteNotification(
            HttpServletRequest request,
            @PathVariable Long productId) {

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 없습니다.");
        }

        String accessToken = auth.substring(7);
        if (jwtProvider.isExpired(accessToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 만료");
        }

        Long uIdx = Long.valueOf(jwtProvider.getUid(accessToken));
        alarmService.deleteNotification(uIdx, productId);
        return ResponseEntity.ok().build();
    }
}