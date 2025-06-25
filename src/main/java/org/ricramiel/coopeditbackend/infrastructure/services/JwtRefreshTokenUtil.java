package org.ricramiel.coopeditbackend.infrastructure.services;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;

@Slf4j
@Service
public class JwtRefreshTokenUtil {

    private final JwtTokenUtil jwtUtil;
    private final SecretKey jwtRefreshSecret;
    private final long jwtExpirationInMs;

    public JwtRefreshTokenUtil(
            JwtTokenUtil jwtUtil,
            @Value("${app.jwt.refresh.secret}") String jwtRefreshSecret,
            @Value("${app.jwt.refresh.expiration-in-ms}")long jwtExpirationInMs
    ) {
        this.jwtUtil = jwtUtil;
        this.jwtRefreshSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret));
        this.jwtExpirationInMs = jwtExpirationInMs;
    }

    public String generateToken(User user) {
        return jwtUtil.generateToken(user.getEmail(), jwtExpirationInMs, jwtRefreshSecret, new HashMap<>());
    }

    public boolean isTokenExpired(String token) {
        return jwtUtil.isTokenExpired(token, jwtRefreshSecret);
    }

    public String extractEmail(String token) {
        return jwtUtil.extractEmail(token, jwtRefreshSecret);
    }

    public Date extractExpiration(String token) {
        return jwtUtil.extractExpiration(token, jwtRefreshSecret);
    }

    public String getValidationErrorMessageOrNull(String authToken) {
        return jwtUtil.getValidationErrorMessageOrNull(authToken, jwtRefreshSecret);
    }
}