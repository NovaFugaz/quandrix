package com.quandrix.ms_auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quandrix.ms_auth.dto.LoginResponse;
import com.quandrix.ms_auth.dto.RegisterRequest;
import com.quandrix.ms_auth.dto.TokenValidationResponse;
import com.quandrix.ms_auth.exception.InvalidCredentialsException;
import com.quandrix.ms_auth.exception.UserAlreadyExistsException;
import com.quandrix.ms_auth.model.Role;
import com.quandrix.ms_auth.model.User;
import com.quandrix.ms_auth.repository.UserRepository;

@Service
public class AuthService {
    
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

    public void register(RegisterRequest request){
        if (userRepository.existsByEmail(request.getEmail())){
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode((request.getPassword())));
        user.setRole((Role.valueOf(request.getRole().toUpperCase())));

        userRepository.save(user);
    }

    public LoginResponse login(String email, String password){
        User user = userRepository.findByEmail(email)
        .orElseThrow(InvalidCredentialsException::new);
    
    if (!passwordEncoder.matches(password, user.getPasswordHash())){
        throw new InvalidCredentialsException(); 
    }

    String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
    return new LoginResponse(token, user.getRole().name(), user.getEmail());

    }

    public TokenValidationResponse validateToken(String token){
        if (!jwtService.isTokenValid(token)){
            return new TokenValidationResponse(null, null, false);
        }
        return new TokenValidationResponse(
            jwtService.extractEmail(token), 
            jwtService.extractRole(token),
            true
        );
    }


}
