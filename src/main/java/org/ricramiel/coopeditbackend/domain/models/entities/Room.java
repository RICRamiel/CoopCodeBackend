package org.ricramiel.coopeditbackend.domain.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.ricramiel.coopeditbackend.domain.models.enums.RoomAccessMode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {
    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;
    @Column(name = "owner_id", insertable=false, updatable=false)
    private UUID ownerId;

    @NotNull
    LocalDateTime creationDate;

    @NotNull
    RoomAccessMode accessMode;

    @NotNull
    @Size(min = 0, max = 100000)
    String code;
}