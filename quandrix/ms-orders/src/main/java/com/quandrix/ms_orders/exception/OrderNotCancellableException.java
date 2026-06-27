package com.quandrix.ms_orders.exception;

public class OrderNotCancellableException extends RuntimeException {
    public OrderNotCancellableException(Long id) {
        super("La orden " + id + " no puede cancelarse en su estado actual");
    }
}