package com.example.bankcards.controller;

import com.example.bankcards.dto.request.UserRequestDto;
import com.example.bankcards.dto.response.UserResponseDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "Управление пользователями (только для ADMIN, кроме профиля)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать пользователя", description = "Доступно только ADMIN")
    public UserResponseDto createUser(@RequestBody UserRequestDto request) {
        User user = userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getRoles()
        );
        return toDto(user);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Обновить пользователя", description = "Доступно только ADMIN")
    public UserResponseDto updateUser(@PathVariable("id") Long id, @RequestBody UserRequestDto request) {
        User user = userService.updateUser(
                id,
                request.getUsername(),
                request.getPassword(),
                request.getRoles()
        );
        return toDto(user);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить пользователя", description = "Доступно только ADMIN")
    public void deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Получить всех пользователей (с пагинацией)", description = "Доступно только ADMIN")
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userService.getAllUsers(pageable).map(this::toDto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Получить профиль пользователя", description = "ADMIN может получить любого, USER — только свой")
    public UserResponseDto getUserProfile(@PathVariable("id") Long id) {
        return toDto(userService.getUserProfile(id));
    }

    private UserResponseDto toDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRoles(
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
        );
        return dto;
    }
}
