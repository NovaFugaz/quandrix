package com.quandrix.ms_auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Datos necesarios para registrar un usuario")
public class RegisterRequest {
    
    @Email(message = "Email inválido")
    @NotBlank(message = "El email es obligatorio")
    @Schema(description = "Correo electronico", example = "Quandrix@gmail.com")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(description = "Contraseña", example = "Quandrix123")
    private String password;

    @NotBlank(message = "El rol es obligatorio")
    @Schema(description = "Rol del usuario", example = "ADMIN-PERSONA-TIENDA")
    private String role;


}
