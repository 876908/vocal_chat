package org.example.vocalchat.infrastructure.service;



import java.security.SecureRandom;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    private static final String VERIFICATION_CODE_PREFIX = "email:verify:";
    private static final long CODE_TTL_MINUTES = 10;
    private static final long RESEND_INTERVAL_SECONDS = 60;

    private final Resend resend;
    private final StringRedisTemplate redisTemplate;
    private final String fromEmail;

    public EmailService(@Value("${resend.api-key}") String apiKey,
                        @Value("${resend.from-email}") String fromEmail,
                        StringRedisTemplate redisTemplate) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
        this.redisTemplate = redisTemplate;
    }


    public void sendVerificationCode(String toEmail) {
    
        String rateLimitKey = VERIFICATION_CODE_PREFIX + "rate:" + toEmail;
        Boolean canSend = redisTemplate.opsForValue()
                .setIfAbsent(rateLimitKey, "1", Duration.ofSeconds(RESEND_INTERVAL_SECONDS));
        if (canSend == null || !canSend) {
            throw new RuntimeException("发送过于频繁，请60秒后再试");
        }

        String code = generateCode();

        String codeKey = VERIFICATION_CODE_PREFIX + toEmail;
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(CODE_TTL_MINUTES));

        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject("VocalChat - 邮箱验证码")
                    .html(buildEmailHtml(code))
                    .build();

            CreateEmailResponse response = resend.emails().send(options);
            log.info("验证码邮件已发送至 {}, id={}", toEmail, response.getId());
        } catch (ResendException e) {
            log.error("发送验证码邮件失败: {}", toEmail, e);
            redisTemplate.delete(codeKey);
            throw new RuntimeException("邮件发送失败，请稍后重试");
        }
    }

    public boolean verifyCode(String email, String code) {
        String codeKey = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return false;
        }
        if (!storedCode.equals(code)) {
            return false;
        }
       
        redisTemplate.delete(codeKey);
        return true;
    }

    
    public boolean hasPendingCode(String email) {
        String codeKey = VERIFICATION_CODE_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(codeKey));
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000; 
        return String.valueOf(code);
    }

    private String buildEmailHtml(String code) {
        return """
                <div style="max-width:600px;margin:0 auto;padding:20px;font-family:Arial,sans-serif;">
                    <h2 style="color:#333;">VocalChat 邮箱验证</h2>
                    <p>您的验证码是：</p>
                    <div style="background:#f5f5f5;padding:20px;text-align:center;margin:20px 0;">
                        <span style="font-size:32px;font-weight:bold;color:#1a73e8;letter-spacing:4px;">%s</span>
                    </div>
                    <p style="color:#666;">验证码 %d 分钟内有效，请勿泄露给他人。</p>
                    <hr style="border:none;border-top:1px solid #eee;margin:20px 0;">
                    <p style="color:#999;font-size:12px;">此邮件由系统自动发送，请勿回复。</p>
                </div>
                """.formatted(code, CODE_TTL_MINUTES);
    }
}
