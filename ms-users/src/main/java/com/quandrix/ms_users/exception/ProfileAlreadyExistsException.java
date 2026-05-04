package com.quandrix.ms_users.exception;

public class ProfileAlreadyExistsException extends RuntimeException {
    public ProfileAlreadyExistsException(Long userId) {
        super("Ya existe un perfil para el userId: " + userId);
    }
}