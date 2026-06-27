package com.quandrix.ms_reviews.exception;

public class ReviewAlreadyExistsException extends RuntimeException{
    public ReviewAlreadyExistsException(Long reviewerId, Long sellerId){
        super("Ya reseñaste a este vendedor. reviewerId="+
            reviewerId + " sellerId=" + sellerId
        );
    }
}
