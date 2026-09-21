package com.thread.Igniter.user.dto;

import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserUpdateDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is Invalid")
    private String email;
    private String bio;
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
       updatedAt = LocalDateTime.now();
    }
}
