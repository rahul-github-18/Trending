package com.thread.Igniter.notification.controller;

import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.notification.dto.NotificationResponseDTO;
import com.thread.Igniter.notification.service.NotificationService;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    @GetMapping
    public List<NotificationResponseDTO> getNotifications(Authentication authentication){
        User user=userRepository.findByUsername(authentication.getName())
                .orElseThrow(()->
                        new ResourceNotFoundException("User does not exist"));
        return notificationService.getNotifications(user.getId());
    }

    @GetMapping("/unread/count")
    public Long getUnread(Authentication authentication){
        User user=userRepository.findByUsername(authentication.getName())
                .orElseThrow(()->
                        new ResourceNotFoundException("User does not exist"));
        return notificationService.getUnreadCount(user.getId());
    }

    @PutMapping("/{notificationId}/read")
    public boolean markAsRead(@PathVariable Long notificationId, Authentication authentication){
        return notificationService.markAsRead(notificationId,authentication.getName());
    }
}
