package com.quandrix.ms_notifications.dto;

import java.time.LocalDateTime;
import com.quandrix.ms_notifications.model.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private NotificationType type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
