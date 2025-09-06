package com.example.bankcards.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "MySuperSecretKeyForJwtToken123456789012345";
    private final long expiration = 1000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, expiration);
    }

    @ParameterizedTest
    @ValueSource(strings = {"testUser1", "AdminUser", "test@example.com"})
    void generateToken_and_extractUsername_success(String username) {
        String token = jwtUtil.generateToken(username);

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(username, jwtUtil.extractUsername(token));
    }

    @Test
    void validateToken_invalidToken_shouldReturnFalse() {
        String invalidToken = "invalid.token.value";

        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void validateToken_expiredToken_shouldReturnFalse() throws InterruptedException {
        JwtUtil shortLivedJwtUtil = new JwtUtil(secret, 1);
        String token = shortLivedJwtUtil.generateToken("expiredUser");

        Thread.sleep(5);

        assertFalse(shortLivedJwtUtil.validateToken(token));
    }

    @Test
    void extractUsername_invalidToken_shouldThrowException() {
        String invalidToken = "broken.token.here";

        assertThrows(Exception.class, () -> jwtUtil.extractUsername(invalidToken));
    }
}
