package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ApiErrorException;
import com.example.bankcards.exception.constant.ErrorStatus;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String password, Set<String> rolesFromRequest) {
        if (userRepository.existsByUsername(username)) {
            throw new ApiErrorException(ErrorStatus.VALIDATION_ERROR);
        }

        Set<Role> mappedRoles = (rolesFromRequest == null || rolesFromRequest.isEmpty())
                ? Collections.singleton(Role.ROLE_USER)
                : rolesFromRequest.stream()
                .map(r -> Role.valueOf("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toSet());

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(mappedRoles);

        return userRepository.save(user);
    }

    public User updateUser(Long userId, String newUsername, String newPassword, Set<String> newRoles) {
        User user = getUserOrThrow(userId);

        if (newUsername != null && !newUsername.isBlank()) {
            if (!newUsername.equals(user.getUsername()) && userRepository.existsByUsername(newUsername)) {
                throw new ApiErrorException(ErrorStatus.VALIDATION_ERROR);
            }
            user.setUsername(newUsername);
        }

        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (newRoles != null && !newRoles.isEmpty()) {
            Set<Role> mappedRoles = newRoles.stream()
                    .map(r -> Role.valueOf("ROLE_" + r.toUpperCase()))
                    .collect(Collectors.toSet());
            user.setRoles(mappedRoles);
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiErrorException(ErrorStatus.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserProfile(Long userId) {
        return getUserOrThrow(userId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiErrorException(ErrorStatus.USER_NOT_FOUND));
    }
}
