package com.thread.Igniter.notification.service;

import com.thread.Igniter.common.exception.ResourceNotFoundException;
import com.thread.Igniter.common.exception.UnauthorizedException;
import com.thread.Igniter.notification.dto.NotificationResponseDTO;
import com.thread.Igniter.notification.entity.Notification;
import com.thread.Igniter.notification.entity.NotificationType;
import com.thread.Igniter.notification.repository.NotificationRepository;
import com.thread.Igniter.user.entity.User;
import com.thread.Igniter.user.repository.UserRepository;
import com.thread.Igniter.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public NotificationResponseDTO createNotification(
            String senderUsername,
            Long recipientId,
            NotificationType type,
            String message,
            Long targetId
    ){
        User sender=userRepository.findByUsername(senderUsername)
                .orElseThrow(()->
                        new ResourceNotFoundException("Username does not exist"));
        User recipient=userRepository.findById(recipientId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Recipient does not exist"));
        Notification notification=new Notification();
        notification.setSender(sender);
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setTargetId(targetId);
        Notification saveNotification=notificationRepository.save(notification);
        NotificationResponseDTO response=new NotificationResponseDTO();
        response.setId(saveNotification.getId());
        response.setSenderId(saveNotification.getSender().getId());
        response.setType(saveNotification.getType());
        response.setSenderUsername(saveNotification.getSender().getUsername());
        response.setSenderProfilePicture(userService.getProfilePictureUrl(saveNotification.getSender().getProfilePicture()));
        response.setMessage(saveNotification.getMessage());
        response.setRead(saveNotification.isRead());
        response.setCreatedAt(saveNotification.getCreatedAt());
        return response;
    }
    public List<NotificationResponseDTO> getNotifications(Long recipientId) {
        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream()
                .map(notify->{
                    NotificationResponseDTO response=new NotificationResponseDTO();
                    response.setId(notify.getId());
                    response.setSenderId(notify.getSender().getId());
                    response.setType(notify.getType());
                    response.setSenderUsername(notify.getSender().getUsername());
                    response.setSenderProfilePicture(userService.getProfilePictureUrl(notify.getSender().getProfilePicture()));
                    response.setMessage(notify.getMessage());
                    response.setRead(notify.isRead());
                    response.setCreatedAt(notify.getCreatedAt());
                    return response;
                }).toList();
    }
    public long getUnreadCount(Long recipientId) {
        return notificationRepository
                .countByRecipientIdAndReadFalse(recipientId);
    }
    @Transactional
    public boolean markAsRead(Long notificationId, String username){
        Notification notification=notificationRepository.findById(notificationId)
                .orElseThrow(()->
                        new ResourceNotFoundException("Notification not found"));

        User user=userRepository.findByUsername(username)
                .orElseThrow(()->
                        new ResourceNotFoundException("User does not exist"));
        if (!(notification.getRecipient().getUsername()).equals(user.getUsername())){
            throw new UnauthorizedException("You are not authorized to read this notification");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
        return true;
    }

    @Transactional
    public void deleteFollowNotification(Long senderId, Long recipientId,Long targetId) {
        notificationRepository.deleteBySenderIdAndRecipientIdAndTypeAndTargetId(senderId, recipientId, NotificationType.FOLLOW,targetId);
    }
    @Transactional
    public void deleteLikeNotification(Long senderId, Long recipientId, Long targetId) {
        notificationRepository.deleteBySenderIdAndRecipientIdAndTypeAndTargetId(senderId, recipientId, NotificationType.LIKE,targetId);
    }
    @Transactional
    public void deleteCommentNotification(Long senderId, Long recipientId,Long targetId) {
        notificationRepository.deleteBySenderIdAndRecipientIdAndTypeAndTargetId(senderId, recipientId, NotificationType.COMMENT,targetId);
    }
}
