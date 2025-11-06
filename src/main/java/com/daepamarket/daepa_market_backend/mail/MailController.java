package com.daepamarket.daepa_market_backend.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {
    private final MailService mailService;

    @PostMapping("/send")
    public ResponseEntity<String> setInviteCrew(@RequestBody Map<String, String> body){
        String email = body.get("email");

        try {
            // ✅ MailService에서 발생한 예외를 여기서 처리합니다.
            mailService.sendMail(email);
            return new ResponseEntity<>("인증 메일 발송 성공", HttpStatus.OK);
        } catch (RuntimeException e) {
            // MailService에서 메일 전송 실패 시 RuntimeException을 던지므로 여기서 잡습니다.
            // 클라이언트에게 500 에러 대신 명확한 메시지를 전달합니다.
            return new ResponseEntity<>("인증 메일 발송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, Object> body){
        String email = body.get("email").toString();
        String code = body.get("code").toString();

        if (email == null || code == null || email.isBlank() || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("verified", false, "msg", "email & code are required"));
        }

        boolean ok = mailService.verifyCode(email, code);

        return ResponseEntity.ok(Map.of("verified", ok));
    }

}
