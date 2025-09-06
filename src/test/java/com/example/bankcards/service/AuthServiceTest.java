package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ApiErrorException;
import com.example.bankcards.exception.constant.ErrorStatus;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_success() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register("john", "pass");

        assertEquals("john", result.getUsername());
        assertEquals("encodedPass", result.getPassword());
        assertEquals(Collections.singleton(Role.ROLE_USER), result.getRoles());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> authService.register("john", "pass"));

        assertEquals(ErrorStatus.VALIDATION_ERROR, ex.getErrorStatus());

        verify(userRepository, never()).save(any(User.class));
    }
}
