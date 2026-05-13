package com.quandrix.ms_transactions.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String identifier) {
        super("Transacción no encontrada: " + identifier);
    }
}