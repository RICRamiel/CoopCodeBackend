package org.ricramiel.coopeditbackend.api.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.api.dtos.JwtModelDto;
import org.ricramiel.coopeditbackend.api.dtos.LoginModelDto;
import org.ricramiel.coopeditbackend.api.dtos.RegisterModelDto;
import org.ricramiel.coopeditbackend.api.dtos.TokenRefreshModelDto;
import org.ricramiel.coopeditbackend.api.mappers.JwtModelMapper;
import org.ricramiel.coopeditbackend.api.mappers.LoginModelMapper;
import org.ricramiel.coopeditbackend.api.mappers.RegisterModelMapper;
import org.ricramiel.coopeditbackend.infrastructure.services.AuthService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final LoginModelMapper loginMapper;
    private final RegisterModelMapper registerMapper;
    private final JwtModelMapper jwtModelMapper;

    @PostMapping("login")
    public JwtModelDto login(@Valid @RequestBody LoginModelDto request) {
        return jwtModelMapper.toDto(
                authService.login(loginMapper.toDomain(request))
        );
    }

    @GetMapping("validate")
    public void isTokenValid() {
    }

    @GetMapping("/oauth2/authorization/{providerId}")
    public void initiateOAuth2Login(
            @Parameter(description = "OAuth2 provider ID (google, github, etc.)", example = "google")
            @PathVariable("providerId") String providerId,
            HttpServletResponse response,
            HttpServletRequest request) throws IOException {
        String redirectUrl = "/oauth2/authorization/" + providerId;

        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty()) {
            redirectUrl = contextPath + redirectUrl;
        }

        response.sendRedirect(redirectUrl);
    }

    @PostMapping("revoke")
    public void revoke(@Valid @RequestBody TokenRefreshModelDto request) {
        authService.revoke(request.getValue());
    }

    @PostMapping("register")
    public JwtModelDto register(@Valid @RequestBody RegisterModelDto request) {
        return jwtModelMapper.toDto(
                authService.register(registerMapper.toDomain(request))
        );
    }

    @PostMapping("refresh")
    public JwtModelDto refreshAndRotate(@Valid @RequestBody TokenRefreshModelDto request) {
        return jwtModelMapper.toDto(
                authService.refreshAndRotate(request.getValue())
        );
    }
}