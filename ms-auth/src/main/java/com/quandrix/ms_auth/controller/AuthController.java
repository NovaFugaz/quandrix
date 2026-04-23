package com.quandrix.ms_auth.controller;

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

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ")
        ? authHeader.substring(7)
        : authHeader;
        return ResponseEntity.ok(authService.validateToken(token));
    }
    
    
}
