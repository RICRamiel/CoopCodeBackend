package org.ricramiel.coopeditbackend.api.mappers;

import org.ricramiel.coopeditbackend.domain.models.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RolesMapper {
    public Role toRole(GrantedAuthority authority) {
        return Role.valueOf(authority.getAuthority());
    }

    public SimpleGrantedAuthority toAuthority(Role role) {
        return new SimpleGrantedAuthority(role.toString());
    }

    public Role toRoleFromString(String role) {
        return Role.valueOf(role);
    }

    public SimpleGrantedAuthority toAuthorityFromString(String role) {
        return new SimpleGrantedAuthority(role);
    }

    public List<SimpleGrantedAuthority> toAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(this::toAuthority)
                .collect(Collectors.toList());
    }

    public Set<Role> toRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(this::toRole)
                .collect(Collectors.toSet());
    }

    public List<String> toStrings(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    public List<String> rolesToStrings(Collection<Role> role) {
        return role.stream()
                .map(Enum::toString)
                .collect(Collectors.toList());
    }

    public List<SimpleGrantedAuthority> toAuthoritiesFromStrings(Collection<String> roles) {
        return roles.stream()
                .map(this::toAuthorityFromString)
                .collect(Collectors.toList());
    }

    public Set<Role> toRolesFromStrings(Collection<String> roles) {
        return roles.stream()
                .map(this::toRoleFromString)
                .collect(Collectors.toSet());
    }
}
