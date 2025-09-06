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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createUser_success() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("john", "pass", Set.of("ADMIN"));

        assertEquals("john", result.getUsername());
        assertEquals("encodedPass", result.getPassword());
        assertTrue(result.getRoles().contains(Role.ROLE_ADMIN));
    }

    @Test
    void createUser_duplicateUsername() {
        when(userRepository.existsByUsername("john")).thenReturn(true);

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> userService.createUser("john", "pass", Set.of("ADMIN")));

        assertEquals(ErrorStatus.VALIDATION_ERROR, ex.getErrorStatus());
    }

    @Test
    void createUser_defaultRoleUser_whenRolesNull() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("john", "pass", null);

        assertTrue(result.getRoles().contains(Role.ROLE_USER));
    }

    @Test
    void createUser_defaultRoleUser_whenRolesEmpty() {
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser("john", "pass", Collections.emptySet());

        assertTrue(result.getRoles().contains(Role.ROLE_USER));
    }

    @Test
    void createUser_invalidRole_shouldThrowException() {
        when(userRepository.existsByUsername("john")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("john", "pass", Set.of("INVALID_ROLE")));
    }

    @Test
    void updateUser_success() {
        User user = new User();
        user.setId(1L);
        user.setUsername("old");
        user.setPassword("oldPass");
        user.setRoles(Set.of(Role.ROLE_USER));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass")).thenReturn("encodedNewPass");
        when(userRepository.existsByUsername("new")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1L, "new", "newPass", Set.of("ADMIN"));

        assertEquals("new", result.getUsername());
        assertEquals("encodedNewPass", result.getPassword());
        assertTrue(result.getRoles().contains(Role.ROLE_ADMIN));
    }

    @Test
    void updateUser_usernameAlreadyExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("new")).thenReturn(true);

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> userService.updateUser(1L, "new", null, null));

        assertEquals(ErrorStatus.VALIDATION_ERROR, ex.getErrorStatus());
    }

    @Test
    void updateUser_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> userService.updateUser(1L, "any", "any", Set.of("ADMIN")));

        assertEquals(ErrorStatus.USER_NOT_FOUND, ex.getErrorStatus());
    }

    @Test
    void updateUser_passwordNull_shouldNotChange() {
        User user = new User();
        user.setId(1L);
        user.setUsername("old");
        user.setPassword("oldPass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("old")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1L, "old", null, null);

        assertEquals("oldPass", result.getPassword());
    }

    @Test
    void updateUser_rolesNull_shouldNotChange() {
        User user = new User();
        user.setId(1L);
        user.setRoles(Set.of(Role.ROLE_USER));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(1L, "new", "newPass", null);

        assertTrue(result.getRoles().contains(Role.ROLE_USER));
    }

    @Test
    void deleteUser_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_notFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> userService.deleteUser(1L));

        assertEquals(ErrorStatus.USER_NOT_FOUND, ex.getErrorStatus());
    }

    @Test
    void getAllUsers_success() {
        List<User> users = List.of(new User(), new User());
        Page<User> page = new PageImpl<>(users);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<User> result = userService.getAllUsers(PageRequest.of(0, 10));

        assertEquals(2, result.getContent().size());
    }

    @Test
    void getUserProfile_success() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserProfile(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getUserProfile_notFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        ApiErrorException ex = assertThrows(ApiErrorException.class,
                () -> userService.getUserProfile(1L));

        assertEquals(ErrorStatus.USER_NOT_FOUND, ex.getErrorStatus());
    }
}
