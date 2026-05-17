package com.quandrix.ms_notifications.dto;

import java.time.LocalDateTime;
import com.quandrix.ms_notifications.model.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private NotificationType type;
    private String message;
    private boolean readFlag;
    private LocalDateTime createdAt;
}
