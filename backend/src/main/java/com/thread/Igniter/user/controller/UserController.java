package com.thread.Igniter.user.controller;

import com.thread.Igniter.user.dto.UserRequestDTO;
import com.thread.Igniter.user.dto.UserResponseDTO;
import com.thread.Igniter.user.dto.UserUpdateDTO;
import com.thread.Igniter.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponseDTO getCurrentUser(Authentication authentication) {
        return userService.getCurrentUser(authentication.getName());
    }

    @PostMapping
    public UserResponseDTO createUser(
            @RequestBody @Valid UserRequestDTO request) {

        return userService.createUser(request);
    }

    @PostMapping("/profile-picture")
    public UserResponseDTO uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        return userService.uploadProfilePicture(
                authentication.getName(),
                file
        );
    }

    @PutMapping("/me")
    public UserResponseDTO updateUser(
            @RequestBody @Valid UserUpdateDTO request,
            Authentication authentication) {

        return userService.updateUser(
                authentication.getName(),
                request
        );
    }
    @GetMapping("/search")
    public List<UserResponseDTO> searchUsers(@RequestParam String text){
        return userService.searchUser(text);
    }
}