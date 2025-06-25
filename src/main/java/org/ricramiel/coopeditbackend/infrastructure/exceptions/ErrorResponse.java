package org.ricramiel.coopeditbackend.infrastructure.exceptions;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ErrorResponse {
    private List<String> errors;

    public ErrorResponse(String error) {
        this.errors = Collections.singletonList(error);
    }

    public ErrorResponse(List<String> errors) {
        this.errors = errors;
    }
}