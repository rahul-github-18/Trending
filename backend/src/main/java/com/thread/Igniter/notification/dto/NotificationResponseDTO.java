package com.thread.Igniter.notification.dto;

import com.thread.Igniter.notification.entity.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponseDTO {

    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderProfilePicture;
    private NotificationType type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}