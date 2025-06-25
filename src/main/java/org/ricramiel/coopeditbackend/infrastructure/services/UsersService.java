package org.ricramiel.coopeditbackend.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ricramiel.coopeditbackend.domain.models.entities.User;
import org.ricramiel.coopeditbackend.domain.models.requests.UserEditRequestModel;
import org.ricramiel.coopeditbackend.infrastructure.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@Slf4j
@RequiredArgsConstructor
public class UsersService{
    private final UserRepository userRepository;

    public User getUserById(@NonNull UUID id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));
    }

    public User editUserById(@NonNull UUID id, @Valid @NonNull UserEditRequestModel userEditModel) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));

        user.setName(userEditModel.getName());

        User saved = userRepository.save(user);
        log.info("Edited user with id: {}", saved.getId());
        return saved;
    }

    public User deleteUserById(@NonNull UUID id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + id));

        user.setActive(false);

        User saved = userRepository.save(user);
        log.info("Deleted user with id: {}", saved.getId());
        return saved;
    }
}
