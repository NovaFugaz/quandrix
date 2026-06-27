package com.quandrix.ms_listings.exception;

public class ListingNotAvailableException extends RuntimeException {
    public ListingNotAvailableException(Long id) {
        super("El listing " + id + " no está disponible");
    }
}