package com.thread.Igniter.thread.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Cursor {
    private Long id;
    private LocalDateTime createdAt;
}
