package com.thread.Igniter.auth.service;

import com.thread.Igniter.auth.dto.LoginRequestDTO;
import com.thread.Igniter.auth.dto.LoginResponseDTO;
import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.security.service.JwtService;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponseDTO login(LoginRequestDTO request) {

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invalid username or password")
                );

        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        );

        if (!passwordMatches) {
            throw new ResourceNotFoundException("Invalid username or password");
        }

        String token = jwtService.generateToken(user.getUsername());

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);

        return response;
    }
    public void logout(String token) {
        jwtService.revokeToken(token);
        SecurityContextHolder.clearContext();
    }
}