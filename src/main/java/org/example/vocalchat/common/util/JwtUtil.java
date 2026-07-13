package org.example.vocalchat.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

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

    public String generateToken(Map<String, Object> claims) {
        String token = Jwts.builder()
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();

        String redisKey = "token:" + token;
        redisTemplate.opsForValue().set(redisKey, "1", expiration, TimeUnit.MILLISECONDS);

        return token;
    }

    public Claims parseJWT(String jwt) {
        String redisKey = "token:" + jwt;
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            throw new RuntimeException("Token 已失效或不存在");
        }

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    public void invalidateToken(String jwt) {
        redisTemplate.delete("token:" + jwt);
    }
}
