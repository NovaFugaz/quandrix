package com.quandrix.ms_users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenValidationResponse {
    private String email;
    private String role;
    private boolean valid;
}