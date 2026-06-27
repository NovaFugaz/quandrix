package com.quandrix.ms_catalog.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(String identifier) {
        super("Carta no encontrada: " + identifier);
    }
}