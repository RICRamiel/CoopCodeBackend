package org.ricramiel.coopeditbackend.domain.models.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserEditRequestModel {
    @NotNull
    @NotBlank
    private String name;
}