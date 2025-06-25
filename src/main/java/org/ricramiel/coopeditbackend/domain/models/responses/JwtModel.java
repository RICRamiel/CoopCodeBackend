package org.ricramiel.coopeditbackend.domain.models.responses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtModel {
    @NotNull
    @NotBlank
    String accessToken;

    @NotNull
    @NotBlank
    private String refreshToken;
}
