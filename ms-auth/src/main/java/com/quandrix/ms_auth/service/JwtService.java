package com.quandrix.ms_auth.service;

import com.quandrix.ms_auth.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
    
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final JwtConfig jwtConfig;

    public JwtService(JwtConfig jwtConfig){
        this.jwtConfig = jwtConfig;
    }

    public String generateToken(String email, String role) {
        log.info("Generando token para email={} rol={}", email, role);
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            log.info("Token validado correctamente");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token inválido: {}", e.getMessage());
            return false;
        }
    }


    public Claims extractClaims(String token){
        return Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractEmail(String token){
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

}
