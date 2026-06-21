package com.quandrix.ms_users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@AllArgsConstructor
public class UserProfileResponse {

    @Schema(description = "Id del perfil de tienda", example = "1")
    private Long id;

    @Schema(description = "Id de usuario", example = "1")
    private Long userId;

    @Schema(description = "Nombre del usuario", example = "persona_123")
    private String displayName;

    @Schema(description = "Fecha de creación del perfil", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;
}