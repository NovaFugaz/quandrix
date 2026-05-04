package com.quandrix.ms_users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class StoreProfileResponse {
    private Long id;
    private Long userId;
    private String storeName;
    private LocalDateTime createdAt;
}