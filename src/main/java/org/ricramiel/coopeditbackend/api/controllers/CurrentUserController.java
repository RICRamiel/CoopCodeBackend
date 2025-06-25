package org.ricramiel.coopeditbackend.api.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ricramiel.coopeditbackend.api.dtos.UserDto;
import org.ricramiel.coopeditbackend.api.dtos.UserEditModelDto;
import org.ricramiel.coopeditbackend.api.mappers.UserEditModelMapper;
import org.ricramiel.coopeditbackend.api.mappers.UserMapper;
import org.ricramiel.coopeditbackend.infrastructure.services.CurrentUserService;
import org.ricramiel.coopeditbackend.infrastructure.services.UsersService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("account")
@RequiredArgsConstructor
@Tag(name = "Account")
public class CurrentUserController {
    private final CurrentUserService currentUserService;
    private final UserMapper userMapper;
    private final UserEditModelMapper userEditModelMapper;
    private final UsersService usersService;

    @GetMapping()
    public UserDto getUser() {
        return userMapper.toDto(currentUserService.getUser());
    }

    @PutMapping("edit")
    public void editUser(@Valid @RequestBody UserEditModelDto model) {
        usersService.editUserById(currentUserService.getId(), userEditModelMapper.toDomain(model));
    }
}