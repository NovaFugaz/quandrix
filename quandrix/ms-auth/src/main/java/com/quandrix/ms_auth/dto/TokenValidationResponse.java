package com.quandrix.ms_auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Token Validation Response", description = "Respuesta de validación de token JWT")
public class TokenValidationResponse {
    
    @Schema(description = "Correo electronico", example = "Quandrix@gmail.com")
    private String email;

    @Schema(description = "Rol del usuario", example = "PERSONA", allowableValues = {"USER", "ADMIN", "PERSONA"})
    private String role;
    
    @Schema(description = "Indica si el token es válido", example = "true")
    private boolean valid;

    
    public TokenValidationResponse(String email, String role, boolean valid) {
        this.email = email;
        this.role = role;
        this.valid = valid;
    }
}