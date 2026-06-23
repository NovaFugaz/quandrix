package com.quandrix.ms_reviews.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationRequest {

    private Long userId;
    private String type;
    private String message;
}
