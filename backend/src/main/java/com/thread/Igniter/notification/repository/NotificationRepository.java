package com.thread.Igniter.notification.repository;

import com.thread.Igniter.notification.entity.Notification;
import com.thread.Igniter.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);
    void deleteBySenderIdAndRecipientIdAndTypeAndTargetId(Long senderId,Long recipientId,NotificationType type,Long targetId);
}