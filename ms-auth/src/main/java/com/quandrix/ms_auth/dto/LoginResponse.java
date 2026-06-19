package com.quandrix.ms_auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta generada al autenticar un usuario correctamente")
public class LoginResponse {
    
    @Schema(description = "Token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Rol del usuario", example = "ADMIN-PERSONA-TIENDA")
    private String role;

    @Schema(description = "Correo electronico", example = "quandrix@gmail.com")
    private String email;
    
}
