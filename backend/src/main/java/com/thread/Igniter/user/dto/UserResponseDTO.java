package com.thread.Igniter.user.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String bio;
    private String profilePicture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}