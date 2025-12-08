package com.example.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    private static final SecretKey key = Keys.hmacShaKeyFor("fe937db6-3509-4197-b6cf-72e7fcd77c60".getBytes(StandardCharsets.UTF_8));
    public static final Date EXPIRE_TIME = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24);
    public static final Date FRESH_TIME = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24);

    public static String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .signWith(key)
                .claims(claims)
                .expiration(EXPIRE_TIME)
                .compact();
    }

    public static String generateRefreshToken(Map<String, Object> claims) {
        return Jwts.builder()
                .signWith(key)
                .claims(claims)
                .expiration(FRESH_TIME)
                .compact();
    }

    public static Claims parseToken(String token) {
        JwtParser jwtParser = Jwts.parser()
                .verifyWith(key)
                .build();
        Jws<Claims> jws = jwtParser.parseSignedClaims(token);
        return jws.getPayload();
    }
}
