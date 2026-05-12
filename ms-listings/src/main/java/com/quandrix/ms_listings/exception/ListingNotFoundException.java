package com.quandrix.ms_listings.exception;

public class ListingNotFoundException extends RuntimeException {
    public ListingNotFoundException(Long id) {
        super("Listing no encontrado con id: " + id);
    }
}