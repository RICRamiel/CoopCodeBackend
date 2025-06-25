package org.ricramiel.coopeditbackend.domain.models.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRequestModel {
    @NotNull
    @Email
    String email;

    @NotNull
    @Size(min = 6, max = 255)
    String password;
}
