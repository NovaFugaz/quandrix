package com.quandrix.ms_auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quandrix.ms_auth.dto.LoginResponse;
import com.quandrix.ms_auth.dto.RegisterRequest;
import com.quandrix.ms_auth.dto.TokenValidationResponse;
import com.quandrix.ms_auth.exception.InvalidCredentialsException;
import com.quandrix.ms_auth.exception.UserAlreadyExistsException;
import com.quandrix.ms_auth.model.Role;
import com.quandrix.ms_auth.model.User;
import com.quandrix.ms_auth.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

        public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

@Transactional
public void register(RegisterRequest request) {
        log.info("Intento de registro para email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registro fallido - email ya existe: {}", request.getEmail());
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        try {
            user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("Rol inválido recibido: {}", request.getRole());
            throw new InvalidCredentialsException();
        }

        userRepository.save(user);
        log.info("Usuario registrado exitosamente con rol={}: {}", 
                user.getRole(), user.getEmail());
    }

    public LoginResponse login(String email, String password) {
        log.info("Intento de login para: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login fallido - usuario no encontrado: {}", email);
                    return new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login fallido - contraseña incorrecta para: {}", email);
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        log.info("Login exitoso para: {} con rol: {}", email, user.getRole());
        return new LoginResponse(token, user.getRole().name(), user.getEmail());
    }

    public TokenValidationResponse validateToken(String token) {
        log.info("Validando token");

        if (!jwtService.isTokenValid(token)) {
            log.warn("Token inválido o expirado");
            return new TokenValidationResponse(null, null, false);
        }

        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);
        log.info("Token válido para email={} rol={}", email, role);
        return new TokenValidationResponse(email, role, true);
    }
}