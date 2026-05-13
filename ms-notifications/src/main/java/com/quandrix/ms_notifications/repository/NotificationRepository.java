package com.quandrix.ms_notifications.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.quandrix.ms_notifications.model.Notification;
import com.quandrix.ms_notifications.model.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>{

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndRead(Long userId, boolean read);

    List<Notification> findByUserIdAndType(Long userId, NotificationType type);

    long countByUserIdAndRead(Long userId, boolean read);

}
