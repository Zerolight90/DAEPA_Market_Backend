package com.daepamarket.daepa_market_backend.review;

import com.daepamarket.daepa_market_backend.common.dto.PagedResponse;
import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.review.dto.MyReviewRow;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewQueryController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ReviewQueryService reviewQueryService;

    private UserEntity resolveUserFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) return null;
        String token = authHeader.substring(7).trim();
        String subject = jwtProvider.getUid(token);
        if (subject == null) return null;

        try {
            Long uIdx = Long.valueOf(subject);
            return userRepository.findById(uIdx).orElse(null);
        } catch (NumberFormatException e) {
            return userRepository.findByUid(subject).orElse(null);
        }
    }

    @GetMapping("/api/review/received")
    public ResponseEntity<?> pageReceived(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserEntity me = resolveUserFromAuth(authHeader);
        if (me == null) return ResponseEntity.status(401).body("인증 정보가 없습니다.");

        PagedResponse<MyReviewRow> resp = reviewQueryService.getReceived(me.getUIdx(), page, size);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/api/review/written")
    public ResponseEntity<?> pageWritten(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserEntity me = resolveUserFromAuth(authHeader);
        if (me == null) return ResponseEntity.status(401).body("인증 정보가 없습니다.");

        PagedResponse<MyReviewRow> resp = reviewQueryService.getWritten(me.getUIdx(), page, size);
        return ResponseEntity.ok(resp);
    }
}
