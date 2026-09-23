package com.thread.Igniter.thread.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThreadResponseDTO {
    private Long id;
    private String content;
    private String username;
    private long likeCount;
    private long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
