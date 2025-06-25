package org.ricramiel.coopeditbackend.infrastructure.services;

import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.api.mappers.RolesMapper;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Slf4j
@Service
public class JwtAccessTokenUtil {
    private static final String CLAIM_KEY_ROLES = "roles";
    private static final String CLAIM_KEY_ID = "id";
    private static final String CLAIM_KEY_NAME = "name";

    private final JwtTokenUtil jwtUtil;
    private final SecretKey jwtAccessSecret;
    private final long jwtExpirationInMs;
    private final RolesMapper rolesMapper;

    public JwtAccessTokenUtil(
            JwtTokenUtil jwtUtil,
            @Value("${app.jwt.access.secret}") String jwtAccessSecret,
            @Value("${app.jwt.access.expiration-in-ms}") long jwtExpirationInMs,
            RolesMapper rolesMapper
    ) {
        this.jwtUtil = jwtUtil;
        this.jwtAccessSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecret));
        this.jwtExpirationInMs = jwtExpirationInMs;
        this.rolesMapper = rolesMapper;
    }

    public Set<Role> extractRoles(String token) {
        return rolesMapper.toRolesFromStrings(
                jwtUtil.<List<String>>extractClaim(token, jwtAccessSecret, CLAIM_KEY_ROLES)
        );
    }

    public UUID extractId(String token) {
        return UUID.fromString(jwtUtil.extractClaim(token, jwtAccessSecret, CLAIM_KEY_ID));
    }

    public String extractName(String token) {
        return jwtUtil.extractClaim(token, jwtAccessSecret, CLAIM_KEY_NAME);
    }

    public String generateToken(User user) {
        var claims = new HashMap<String, Object>();

        List<String> roles = rolesMapper.rolesToStrings(user.getRoles());
        claims.put(CLAIM_KEY_ROLES, roles);
        claims.put(CLAIM_KEY_ID, user.getId());
        claims.put(CLAIM_KEY_NAME, user.getName());

        return jwtUtil.generateToken(user.getEmail(), jwtExpirationInMs, jwtAccessSecret, claims);
    }

    private Boolean isTokenExpired(String token) {
        return jwtUtil.isTokenExpired(token, jwtAccessSecret);
    }

    public String extractEmail(String token) {
        return jwtUtil.extractEmail(token, jwtAccessSecret);
    }

    public Date extractExpiration(String token) {
        return jwtUtil.extractExpiration(token, jwtAccessSecret);
    }

    public String getValidationErrorMessageOrNull(String authToken) {
        return jwtUtil.getValidationErrorMessageOrNull(authToken, jwtAccessSecret);
    }

    public boolean isTokenValid(String authToken) {
        return jwtUtil.getValidationErrorMessageOrNull(authToken, jwtAccessSecret) == null;
    }
}