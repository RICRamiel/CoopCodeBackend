package org.ricramiel.coopeditbackend.infrastructure.exceptions;

import lombok.Data;

import java.util.List;

@Data
public class DevelopmentServerErrorResponse {
    private List<String> productionResponse;
    private String message;
    private List<String> stacktrace;

    DevelopmentServerErrorResponse(List<String> productionResponse, String message, List<String> stacktrace) {
        this.productionResponse = productionResponse;
        this.message = message;
        this.stacktrace = stacktrace;
    }

    DevelopmentServerErrorResponse(String productionResponse, String message, List<String> stacktrace) {
        this.productionResponse = List.of(productionResponse);
        this.message = message;
        this.stacktrace = stacktrace;
    }

    DevelopmentServerErrorResponse(ErrorResponse productionResponse, String message, List<String> stacktrace) {
        this.productionResponse = productionResponse.getErrors();
        this.message = message;
        this.stacktrace = stacktrace;
    }
}