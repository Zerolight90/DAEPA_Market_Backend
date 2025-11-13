// src/main/java/com/daepamarket/daepa_market_backend/naga/NagaController.java
package com.daepamarket.daepa_market_backend.naga;

import com.daepamarket.daepa_market_backend.domain.user.UserEntity;
import com.daepamarket.daepa_market_backend.domain.user.UserRepository;
import com.daepamarket.daepa_market_backend.jwt.JwtProvider;
import com.daepamarket.daepa_market_backend.naga.dto.NagaReportRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/naga")
public class NagaController {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final NagaService nagaService;

    private UserEntity resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) return null;
        String token = authHeader.substring(7).trim();
        String subject = jwtProvider.getUid(token);
        if (subject == null) return null;
        try {
            Long id = Long.valueOf(subject);
            return userRepository.findById(id).orElse(null);
        } catch (NumberFormatException e) {
            return userRepository.findByUid(subject).orElse(null);
        }
    }

    @PostMapping("/report")
    public ResponseEntity<?> report(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody NagaReportRequest req
    ) {
        UserEntity me = resolveUser(authHeader);
        if (me == null) return ResponseEntity.status(401).body("인증 정보가 없습니다.");
        if (req.getProductId() == null || req.getNgStatus() == null)
            return ResponseEntity.badRequest().body("잘못된 요청입니다.");

        Long ngIdx = nagaService.report(me, req);
        return ResponseEntity.ok(ngIdx);
    }
}
