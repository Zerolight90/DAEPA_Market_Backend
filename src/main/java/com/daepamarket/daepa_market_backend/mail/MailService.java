/*
package com.daepamarket.daepa_market_backend.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    //발신자 이름
    private final String SENDER_NAME = "대파마켓";

    public void sendMail(String toEmail) {

        //인증키 생성
        String authKey = createKey();

        // 제목 및 본문 생성
        String subject = "대파마켓 인증번호";
        String htmlContent = createAuthHtmlContent(authKey);

        // 3. MimeMessage 객체 생성
        MimeMessage message = emailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            emailSender.send(message);
            log.info("인증 메일 발송 성공: 수신자 {}", toEmail);
        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", toEmail, e);

            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

        //8자리 인증키 생성
        private String createKey() {
            Random random = new Random();
            StringBuilder key = new StringBuilder();
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

            for (int i = 0; i < 8; i++) {
                key.append(chars.charAt(random.nextInt(chars.length())));
            }
            return key.toString();

        }

    private String createAuthHtmlContent(String key) {
        return "<div style='margin:20px; font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #4CAF50;'>대파마켓 이메일 인증 안내</h2><br>" +
                "<p>아래 <strong>인증 코드</strong>를 복사하여 인증 화면에 입력해 주세요.</p>" +
                "<div style='background-color: #f2f2f2; padding: 20px; text-align: center; border-radius: 5px;'>" +
                "<h3 style='color: #333;'>인증 코드</h3>" +
                "<div style='font-size:150%; font-weight:bold; color: #007bff;'>" + key + "</div>" +
                "</div>" +
                "<br><p>감사합니다.</p>" +
                "</div>";
    }

}
*/
