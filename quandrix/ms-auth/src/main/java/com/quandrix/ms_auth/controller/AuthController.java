package com.quandrix.ms_auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quandrix.ms_auth.dto.LoginRequest;
import com.quandrix.ms_auth.dto.LoginResponse;
import com.quandrix.ms_auth.dto.RegisterRequest;
import com.quandrix.ms_auth.dto.TokenValidationResponse;
import com.quandrix.ms_auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/auth")
@Tag(name = "Autentificación", description = "Operaciones relacionadas con la autentificaión de usuarios.")
public class AuthController {

    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar un usuario", description = "Crear un nuevo usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuario registrado con exito"),
        @ApiResponse(responseCode = "400", description = "Datos de registro inválidos"),
        @ApiResponse(responseCode = "409", description = "El correo ya está registrado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register email={}", request.getEmail());
        authService.register(request);
        log.info("Registro completado para: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica un usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login exitoso",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de login inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login email={}", request.getEmail());
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        log.info("Login exitoso para: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    @Operation(summary = "Validar token JWT", description = "Valida token JWT y retorna su estado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validación completada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenValidationResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Token inválido o mal generado"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<TokenValidationResponse> validate(
            @Parameter(description = "Token JWT en formate Bearer", required = true, example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
            @RequestHeader("Authorization") String authHeader) {
        log.info("GET /auth/validate");
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : authHeader;
        TokenValidationResponse response = authService.validateToken(token);
        log.info("Validación completada - válido={}", response.isValid());
        return ResponseEntity.ok(response);
    }
}