package org.ricramiel.coopeditbackend.infrastructure.services;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.domain.models.entities.RefreshToken;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.ricramiel.coopeditbackend.domain.models.requests.LoginRequestModel;
import org.ricramiel.coopeditbackend.domain.models.requests.RegisterRequestModel;
import org.ricramiel.coopeditbackend.domain.models.responses.JwtModel;
import org.ricramiel.coopeditbackend.infrastructure.exceptions.AuthException;
import org.ricramiel.coopeditbackend.infrastructure.repositories.RefreshTokenRepository;
import org.ricramiel.coopeditbackend.infrastructure.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;
import java.util.Set;

@Service
@Validated
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtAccessTokenUtil accessTokenUtil;
    private final JwtRefreshTokenUtil refreshTokenUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtRefreshTokenUtil jwtRefreshTokenUtil;
    private final JwtAccessTokenUtil jwtAccessTokenUtil;

    public boolean isTokenValid(@NonNull String tokenValue) {
        return jwtAccessTokenUtil.isTokenValid(tokenValue);
    }

    public JwtModel login(@Valid @NonNull LoginRequestModel authRequest) {
        User user = userRepository.findByEmailAndIsActiveIsTrue(authRequest.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User with email " + authRequest.getEmail() + " not found"));

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            log.info("Unsuccessful login for user with email {}", authRequest.getEmail());
            throw new AuthException("Wrong password");
        }

        JwtModel saved = createAndSaveJwtToken(user);
        log.info("User with id {} logged in", user.getId());
        return saved;
    }

    public void revoke(@NonNull String refreshTokenValue) {
        final RefreshToken savedRefreshToken = refreshTokenRepository
                .findByValue(refreshTokenValue)
                .orElseThrow(() -> new EntityNotFoundException("Refresh token not found with value: " + refreshTokenValue));

        refreshTokenRepository.delete(savedRefreshToken);
    }

    public JwtModel register(@Valid @NonNull RegisterRequestModel authRequest) {
        if (userRepository.existsByEmail(authRequest.getEmail())){
            throw new AuthException("Email already in use");
        }

        User user = User
                .builder()
                .email(authRequest.getEmail())
                .name(authRequest.getName())
                .password(passwordEncoder.encode(authRequest.getPassword()))
                .roles(Set.of(Role.USER))
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("User with email {} registered", authRequest.getEmail());

        return createAndSaveJwtToken(user);
    }

    @Transactional
    public JwtModel refreshAndRotate(@Valid @NonNull String refreshTokenValue) {
        final RefreshToken savedRefreshToken = refreshTokenRepository
                .findByValue(refreshTokenValue)
                .orElseThrow(() -> new EntityNotFoundException("Refresh token not found with value: " + refreshTokenValue));

        final User user = savedRefreshToken.getUser();
        if (!user.isActive()){
            throw new AuthException("User is deleted");
        }

        String newAccessTokenValue = accessTokenUtil.generateToken(user);
        String newRefreshTokenValue = refreshTokenUtil.generateToken(user);

        savedRefreshToken.setValue(newRefreshTokenValue);
        savedRefreshToken.setExtractedExpiryDate(jwtRefreshTokenUtil.extractExpiration(newRefreshTokenValue));
        refreshTokenRepository.save(savedRefreshToken);

        return new JwtModel(newAccessTokenValue, newRefreshTokenValue);
    }

    public JwtModel createAndSaveJwtToken(@Valid @NonNull User user) {
        String accessToken = accessTokenUtil.generateToken(user);
        String refreshToken = refreshTokenUtil.generateToken(user);

        RefreshToken refreshTokenModel = RefreshToken.builder()
                .user(user)
                .value(refreshToken)
                .extractedExpiryDate(jwtRefreshTokenUtil.extractExpiration(refreshToken))
                .build();

        refreshTokenRepository.save(refreshTokenModel);

        return new JwtModel(accessToken, refreshToken);
    }
}