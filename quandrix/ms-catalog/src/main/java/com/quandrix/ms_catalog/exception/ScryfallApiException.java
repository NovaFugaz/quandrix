package com.quandrix.ms_catalog.exception;

public class ScryfallApiException extends RuntimeException {
    public ScryfallApiException(String message) {
        super("Error al consultar Scryfall: " + message);
    }
}