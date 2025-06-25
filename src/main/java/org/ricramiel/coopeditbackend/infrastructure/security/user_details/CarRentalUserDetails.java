package org.ricramiel.coopeditbackend.infrastructure.security.user_details;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class CarRentalUserDetails implements UserDetails {
    private UUID id;
    private String email;
    private String name;
    private String password;
    private boolean isActive;
    private Collection<GrantedAuthority> authorities;

    public CarRentalUserDetails(User user) {
        id = user.getId();
        email = user.getEmail();
        name = user.getName();
        password = user.getPassword();
        authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
        isActive = user.isActive();
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}