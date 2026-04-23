package com.quandrix.ms_auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenValidationResponse {
    
    private String email;
    private String role;
    private boolean valid;

    
    public TokenValidationResponse(String email, String role, boolean valid) {
        this.email = email;
        this.role = role;
        this.valid = valid;
    }
}