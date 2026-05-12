package com.quandrix.ms_payments.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(Long orderId) {
        super("Pago no encontrado para orderId: " + orderId);
    }
}