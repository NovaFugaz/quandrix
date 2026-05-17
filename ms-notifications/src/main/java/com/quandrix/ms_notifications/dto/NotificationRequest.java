package com.quandrix.ms_notifications.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotNull(message = "El userId es obligatorio")
    private Long userId;

    @NotBlank(message = "El tipo es obligatorio")
    private String type;

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;
}
