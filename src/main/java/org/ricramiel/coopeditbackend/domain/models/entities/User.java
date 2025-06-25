package org.ricramiel.coopeditbackend.domain.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ricramiel.coopeditbackend.domain.models.enums.Role;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @NotNull
    @NotBlank
    private String name;

    @Column(unique = true)
    @NotNull
    @NotBlank
    private String email;

    @Size(min = 6, max = 255)
    private String password;

    private boolean isActive = true;

    @OneToMany(mappedBy = "user")
    private List<OAuth2Provider> providers;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    private Set<Role> roles = new HashSet<>();
}