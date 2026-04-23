package com.quandrix.ms_auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    
    private String token;
    private String role;
    private String email;

    public LoginResponse(String token, String role, String email){
        this.token = token;
        this.role = role;
        this.email = email;
    }
}
