package org.ricramiel.coopeditbackend.infrastructure.exceptions;

import org.ricramiel.coopeditbackend.infrastructure.exceptions.status_code_exceptions.UnauthorizedException;

public class AuthException extends UnauthorizedException {
    public AuthException(String message) {
        super(message);
    }
}