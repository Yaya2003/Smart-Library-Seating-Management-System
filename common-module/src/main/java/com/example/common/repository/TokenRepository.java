package com.example.common.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TokenRepository {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public Boolean hasToken(String token) {
        return redisTemplate.hasKey(token);
    }

    public void removeToken(String token) {
        redisTemplate.delete(token);
    }

    public void saveToken(String token) {
        redisTemplate.opsForValue().set(token, token, 24, TimeUnit.HOURS);
    }
}
