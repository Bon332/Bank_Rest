package com.example.bankcards.controller;

import com.example.bankcards.dto.request.UserRequestDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ApiErrorException;
import com.example.bankcards.exception.constant.ErrorStatus;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUser_success() throws Exception {
        UserRequestDto request = new UserRequestDto("john", "password", Set.of("ROLE_USER"));
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setRoles(Set.of(Role.ROLE_USER));

        when(userService.createUser(eq("john"), eq("password"), eq(Set.of("ROLE_USER"))))
                .thenReturn(user);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void createUser_validationError() throws Exception {
        UserRequestDto request = new UserRequestDto("john", "password", Set.of("ROLE_USER"));

        when(userService.createUser(any(), any(), any()))
                .thenThrow(new ApiErrorException(ErrorStatus.VALIDATION_ERROR));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Validation failed"));
    }

    @Test
    void updateUser_success() throws Exception {
        UserRequestDto request = new UserRequestDto("newname", "newpass", Set.of("ROLE_ADMIN"));
        User user = new User();
        user.setId(1L);
        user.setUsername("newname");
        user.setRoles(Set.of(Role.ROLE_ADMIN));

        when(userService.updateUser(eq(1L), eq("newname"), eq("newpass"), eq(Set.of("ROLE_ADMIN"))))
                .thenReturn(user);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("newname"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void updateUser_notFound() throws Exception {
        UserRequestDto request = new UserRequestDto("newname", "newpass", Set.of("ROLE_ADMIN"));

        when(userService.updateUser(eq(1L), any(), any(), any()))
                .thenThrow(new ApiErrorException(ErrorStatus.USER_NOT_FOUND));

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found"));
    }

    @Test
    void deleteUser_success() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUser_notFound() throws Exception {
        doThrow(new ApiErrorException(ErrorStatus.USER_NOT_FOUND))
                .when(userService).deleteUser(eq(1L));

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value(ErrorStatus.USER_NOT_FOUND.getMessage()));
    }

    @Test
    void getAllUsers_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setRoles(Set.of(Role.ROLE_USER));

        Page<User> page = new PageImpl<>(List.of(user));
        when(userService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].username").value("john"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("ROLE_USER"));
    }

    @Test
    void getUserProfile_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setRoles(Set.of(Role.ROLE_USER));

        when(userService.getUserProfile(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    void getUserProfile_notFound() throws Exception {
        when(userService.getUserProfile(1L))
                .thenThrow(new ApiErrorException(ErrorStatus.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorMessage").value("User not found"));
    }
}
