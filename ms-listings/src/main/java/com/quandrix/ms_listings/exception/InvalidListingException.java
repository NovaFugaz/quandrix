package com.quandrix.ms_listings.exception;

public class InvalidListingException extends RuntimeException {
    public InvalidListingException(String message) {
        super(message);
    }
}