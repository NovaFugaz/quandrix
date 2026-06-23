package com.quandrix.ms_auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = "Login Request", description = "Datos necesarios para Iniciar sesión")
public class LoginRequest {
    
    @Email
    @NotBlank
    @Schema(description = "Correo electronico", example = "Quandrix@gmail.com")
    private String email;

    @NotBlank
    @Schema(description = "Contraseña", example = "Quandrix123")
    private String password;

}
