package org.ricramiel.coopeditbackend.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtModelDto {
    @NotNull
    @NotBlank
    String accessToken;

    @NotNull
    @NotBlank
    private String refreshToken;
}
