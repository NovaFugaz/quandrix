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

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /auth/register email={}", request.getEmail());
        authService.register(request);
        log.info("Registro completado para: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /auth/login email={}", request.getEmail());
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        log.info("Login exitoso para: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(
            @RequestHeader("Authorization") String authHeader) {
        log.info("GET /auth/validate");
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7) : authHeader;
        TokenValidationResponse response = authService.validateToken(token);
        log.info("Validación completada - válido={}", response.isValid());
        return ResponseEntity.ok(response);
    }
}