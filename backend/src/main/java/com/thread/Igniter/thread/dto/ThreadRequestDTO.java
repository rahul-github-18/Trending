package com.thread.Igniter.thread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ThreadRequestDTO {
    @NotBlank(message = "Content is required")
    @Size(max = 500,message = "Size must not exceed 500 character")
    private String content;
}
