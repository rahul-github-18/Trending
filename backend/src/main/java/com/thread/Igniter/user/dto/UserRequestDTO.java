package com.thread.Igniter.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequestDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 10, message = "Username is must be between 3 and 10 character")
    private String username;


    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 4,message = "Password must be minimum of 4 character")
    private String password;

    @Size(max = 500,message = "Bio must not exceed 500 character")
    private String bio;
}
