package com.quandrix.ms_users.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("No se encontró perfil para el userId: " + userId);
    }
}