package com.thread.Igniter.testing.unit;

import com.thread.Igniter.auth.dto.LoginRequestDTO;
import com.thread.Igniter.auth.dto.LoginResponseDTO;
import com.thread.Igniter.auth.service.AuthService;
import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.security.service.JwtService;
import com.thread.Igniter.security.service.RedisTokenService;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RedisTokenService redisTokenService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");

        loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("rawPassword");
    }

    @Test
    @DisplayName("Should successfully login and return JWT token")
    void testLoginSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken("testuser")).thenReturn("mock-jwt-token");

        LoginResponseDTO response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("rawPassword", "encodedPassword");
        verify(jwtService).generateToken("testuser");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user is not found")
    void testLoginUserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when password does not match")
    void testLoginWrongPassword() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> authService.login(loginRequest));
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Should revoke token and clear context on logout")
    void testLogout() {
        String token = "sample-token-to-revoke";
        Date exp = new Date(System.currentTimeMillis() + 10000);
        when(jwtService.extractJti(token)).thenReturn("mock-jti");
        when(jwtService.extractExpiration(token)).thenReturn(exp);

        authService.logout(token);

        verify(jwtService).extractJti(token);
        verify(jwtService).extractExpiration(token);
        verify(redisTokenService).revokeToken("mock-jti", exp);
    }
}
