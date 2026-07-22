package org.example.vocalchat.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Component
public class JwtUtil {

    private static final long DEFAULT_TTL_SECONDS = 86400;

    private final SecretKey key;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration}")
    private long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   StringRedisTemplate redisTemplate) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(String userId) {
        long ttlMillis = expiration > 0 ? expiration : DEFAULT_TTL_SECONDS * 1000;

        String token = Jwts.builder()
                .subject(userId)
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(key)
                .compact();

        redisTemplate.opsForValue().set("user:" + userId, token, Duration.ofMillis(ttlMillis));

        return token;
    }

    public Claims parseJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String userId = claims.getSubject();
        String stored = redisTemplate.opsForValue().get("user:" + userId);
        if (stored == null || !stored.equals(token)) {
            throw new BaseException(ErrorEnum.TOKEN_EXPIRED);
        }

        return claims;
    }

    public void invalidateToken(String userId) {
        redisTemplate.delete("user:" + userId);
    }
}
