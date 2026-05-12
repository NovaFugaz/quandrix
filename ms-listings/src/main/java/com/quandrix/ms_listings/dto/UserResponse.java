package com.quandrix.ms_listings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private Long userId;
    private String displayName;
}