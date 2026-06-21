package com.quandrix.ms_users.exception;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException() {
        super("No tienes permisos para realizar esta acción");
    }
}