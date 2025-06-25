package org.ricramiel.coopeditbackend.infrastructure.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtTokenUtil {
    public String generateToken(
            @NonNull String email,
            long jwtExpirationInMs,
            @NonNull SecretKey secretKey,
            @NonNull Map<String, Object> claims
    ) {
        Date nowDate = new Date();
        Date expiryDate = new Date(nowDate.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(nowDate)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public Boolean isTokenExpired(String token, SecretKey secretKey) {
        return extractExpiration(token, secretKey).before(new Date());
    }

    public String extractEmail(String token, SecretKey secretKey) {
        return extractClaim(token, secretKey, Claims::getSubject);
    }

    public Date extractExpiration(String token, SecretKey secretKey) {
        return extractClaim(token, secretKey, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, SecretKey secretKey, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token, secretKey);
        return claimsResolver.apply(claims);
    }

    public <T> T extractClaim(String token, SecretKey secretKey, String key) {
        final Claims claims = extractAllClaims(token, secretKey);
        return (T) claims.get(key);
    }

    private Claims extractAllClaims(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getValidationErrorMessageOrNull(String token, SecretKey secretKey){
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }
}