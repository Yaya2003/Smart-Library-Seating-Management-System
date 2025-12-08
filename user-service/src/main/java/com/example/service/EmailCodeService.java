package com.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class EmailCodeService {

    private static final String CODE_KEY_PREFIX = "email:code:";
    private static final String SEND_LIMIT_KEY_PREFIX = "email:code:limit:";

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public EmailCodeService(StringRedisTemplate redisTemplate, JavaMailSender mailSender) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
    }

    public boolean isLimited(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SEND_LIMIT_KEY_PREFIX + email));
    }

    public void cacheLimit(String email) {
        redisTemplate.opsForValue().set(SEND_LIMIT_KEY_PREFIX + email, "1", 60, TimeUnit.SECONDS);
    }

    public void storeCode(String email, String code) {
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + email, code, 5, TimeUnit.MINUTES);
    }

    public boolean verifyCode(String email, String code) {
        String cachedCode = redisTemplate.opsForValue().get(CODE_KEY_PREFIX + email);
        return cachedCode != null && cachedCode.equals(code);
    }

    public void clearCode(String email) {
        redisTemplate.delete(CODE_KEY_PREFIX + email);
    }

    public String generateCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1000000));
    }

    public void sendCode(String email) {
        String code = generateCode();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("验证码");
        message.setText("验证码：" + code + "，5分钟有效");

        mailSender.send(message);
        storeCode(email, code);
        cacheLimit(email);
    }
}
