package com.example.bankcards.controller;

import com.example.bankcards.dto.request.AuthRequestDto;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.dto.response.RegisterResponseDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequestDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

            String token = jwtUtil.generateToken(userDetails.getUsername());

            return new AuthResponse(token);
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверные учетные данные");
        }
    }


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDto register(@RequestBody AuthRequestDto request) {
        User user = authService.register(request.getUsername(), request.getPassword());
        return new RegisterResponseDto(
                "Регистрация прошла успешно",
                user.getId(),
                user.getUsername()
        );
    }

}
