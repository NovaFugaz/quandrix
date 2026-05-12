package com.quandrix.ms_transactions.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(Long orderId) {
        super("Ya existe una transacción para la orden: " + orderId);
    }
}