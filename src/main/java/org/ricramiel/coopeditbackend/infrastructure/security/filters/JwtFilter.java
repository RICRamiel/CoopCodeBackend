package org.ricramiel.coopeditbackend.infrastructure.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.api.mappers.RolesMapper;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.ricramiel.coopeditbackend.infrastructure.services.JwtAccessTokenUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private final RolesMapper rolesMapper;
    private final JwtAccessTokenUtil accessTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws IOException, ServletException {
        String token = getTokenFromRequestOrNull(request);
        SetContextAuthentication(token, response);
        chain.doFilter(request, response);
    }

    private void SetContextAuthentication(String token, HttpServletResponse response) {
        if (token == null){
            log.debug("Token is not present");
            return;
        }
        String error = accessTokenService.getValidationErrorMessageOrNull(token);
        if (error != null) {
            log.warn("Token is present, but not valid. Token: {}, cause: {}", token, error);
            return;
        }

        List<SimpleGrantedAuthority> authorities = rolesMapper.toAuthorities(
                accessTokenService.extractRoles(token)
        );

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(token, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        log.debug("Token is valid and security context holder is filled");
    }

    private String getTokenFromRequestOrNull(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}