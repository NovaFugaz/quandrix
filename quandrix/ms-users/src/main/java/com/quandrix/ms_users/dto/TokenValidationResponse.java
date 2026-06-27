package com.quandrix.ms_users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Token Validation Response", description = "Respuesta de validación de token JWT")
public class TokenValidationResponse {
    @Schema(description = "Correo electronico", example = "quandrix@gmail.com")
    private String email;

    @Schema(description = "Rol del usuario", example = "ADMIN", allowableValues = {"ADMIN", "PERSONA", "TIENDA"})
    private String role;

    @Schema(description = "Indica si el token es válido", example = "true")
    private boolean valid;
}