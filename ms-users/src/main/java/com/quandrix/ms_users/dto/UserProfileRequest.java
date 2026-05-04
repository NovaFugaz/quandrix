package com.quandrix.ms_users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileRequest {

    @NotNull(message = "El userId es obligatorio")
    private Long userId;

    @NotBlank(message = "El nombre es obligatorio")
    private String displayName;

}