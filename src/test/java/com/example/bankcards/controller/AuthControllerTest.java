package com.example.bankcards.controller;

import com.example.bankcards.dto.request.AuthRequestDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ApiErrorException;
import com.example.bankcards.exception.constant.ErrorStatus;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.security.JwtUtil;
import com.example.bankcards.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/login — успешный логин возвращает токен")
    void login_Success() throws Exception {
        AuthRequestDto request = new AuthRequestDto("user1", "password123");

        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User(
                        "user1",
                        "password123",
                        Collections.emptyList()
                );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUtil.generateToken("user1")).thenReturn("mock-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — неверные данные возвращают 401")
    void login_Failure_BadCredentials() throws Exception {
        AuthRequestDto request = new AuthRequestDto("user1", "wrong-pass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorMessage").value("Неверные учетные данные"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — успешная регистрация возвращает пользователя")
    void register_Success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("newUser");

        when(authService.register(eq("newUser"), eq("password123"))).thenReturn(user);

        AuthRequestDto request = new AuthRequestDto("newUser", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Регистрация прошла успешно"))
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.username").value("newUser"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — регистрация уже существующего пользователя возвращает 400")
    void register_Failure_UserAlreadyExists() throws Exception {
        AuthRequestDto request = new AuthRequestDto("existingUser", "password123");

        when(authService.register(eq("existingUser"), eq("password123")))
                .thenThrow(new ApiErrorException(ErrorStatus.VALIDATION_ERROR));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
