package org.ricramiel.coopeditbackend.infrastructure.services;

import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.ricramiel.coopeditbackend.infrastructure.exceptions.AuthException;
import org.ricramiel.coopeditbackend.infrastructure.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService{

    private final UserRepository userRepository;
    private final JwtAccessTokenUtil accessTokenUtil;

    public User getUser() {
        return userRepository
                .findByEmail(getEmail())
                .orElseThrow(() -> new AuthException("Unauthorized"));
    }

    public String getEmail() {
        return accessTokenUtil.extractEmail(getToken());
    }

    public UUID getId() {
        return accessTokenUtil.extractId(getToken());
    }

    private String getToken(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication.getPrincipal() instanceof String jwt) {
            return jwt;
        }

        throw new IllegalStateException("Invalid authentication type");
    }
}
