package com.example.papersystem.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtUtil {
    private static final Key KEY = Keys.hmacShaKeyFor("my_super_secret_key_for_papersystem_123456!".getBytes());
    private static final long EXPIRE = 1000 * 60 * 60 * 24; // 24小时过期

    public static String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(KEY)
                .compact();
    }

    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            return null;
        }
    }
}