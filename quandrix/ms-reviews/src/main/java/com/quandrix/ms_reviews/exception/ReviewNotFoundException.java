package com.quandrix.ms_reviews.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(Long id) {
        super("Reseña no encontrada con id: " + id);
    }
}