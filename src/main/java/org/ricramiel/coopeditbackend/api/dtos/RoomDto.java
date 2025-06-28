package org.ricramiel.coopeditbackend.api.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAccessMode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDto {
    @NotNull
    private UUID id;

    private UUID ownerId;

    private String name;

    @NotNull
    LocalDateTime creationDate;

    @NotNull
    RoomAccessMode accessMode;
}
