package com.quandrix.ms_orders.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationRequest {
    private Long userId;
    private String type;
    private String message;
}