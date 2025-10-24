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
        mailService.sendMail(body.get("u_id"));
        return new ResponseEntity<>("초대 메일 발송 성공", HttpStatus.OK);
    }

}
