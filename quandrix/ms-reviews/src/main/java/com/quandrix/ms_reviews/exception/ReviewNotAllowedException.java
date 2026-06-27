package com.quandrix.ms_reviews.exception;

public class ReviewNotAllowedException extends RuntimeException {
    public ReviewNotAllowedException(){
        super("No puedes reseñar a un vendedor sin haber completado la compra.");
    }

}
