package com.quandrix.ms_notifications.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quandrix.ms_notifications.model.Notification;
import com.quandrix.ms_notifications.model.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long>{

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndReadFlag(Long userId, boolean readFlag);

    List<Notification> findByUserIdAndType(Long userId, NotificationType type);

    long countByUserIdAndReadFlag(Long userId, boolean readFlag);

}
