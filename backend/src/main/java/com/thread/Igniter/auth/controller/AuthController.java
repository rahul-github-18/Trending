package com.thread.Igniter.auth.controller;

import com.thread.Igniter.auth.dto.LoginRequestDTO;
import com.thread.Igniter.auth.dto.LoginResponseDTO;
import com.thread.Igniter.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody @Valid LoginRequestDTO request){
        return authService.login(request);
    }

    @PostMapping("/logout")
    public String logout(@RequestHeader("Authorization") String authHeader){
        String token=authHeader.substring(7);
        authService.logout(token);
        return "Logged Out SuccessFully";
    }
}
