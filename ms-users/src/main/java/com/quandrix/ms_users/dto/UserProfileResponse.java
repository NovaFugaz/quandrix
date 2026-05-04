package com.quandrix.ms_users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private Long userId;
    private String displayName;
    private LocalDateTime createdAt;
}