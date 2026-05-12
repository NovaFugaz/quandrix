package com.quandrix.ms_payments.service;

import com.quandrix.ms_payments.dto.PaymentRequest;
import com.quandrix.ms_payments.dto.PaymentResponse;
import com.quandrix.ms_payments.exception.PaymentNotFoundException;
import com.quandrix.ms_payments.exception.PaymentProcessingException;
import com.quandrix.ms_payments.model.Payment;
import com.quandrix.ms_payments.model.PaymentMethod;
import com.quandrix.ms_payments.model.PaymentStatus;
import com.quandrix.ms_payments.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse process(PaymentRequest request) {
        log.info("Procesando pago para orderId={} monto={}",
                request.getOrderId(), request.getAmount());

        // Un mismo orderId no puede tener dos pagos
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            log.warn("Ya existe un pago para orderId={}", request.getOrderId());
            throw new PaymentProcessingException(
                    "Ya existe un pago registrado para la orden " + request.getOrderId());
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException(
                    "Método de pago inválido: " + request.getMethod() +
                            ". Valores válidos: CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER, CASH");
        }

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setMethod(method);

        // Simulación: siempre aprueba en desarrollo
        // En producción aquí iría la integración con pasarela real
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        log.info("Pago procesado exitosamente id={} status={}",
                saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    public PaymentResponse getByOrderId(Long orderId) {
        log.info("Buscando pago para orderId={}", orderId);
        return toResponse(paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("Pago no encontrado para orderId={}", orderId);
                    return new PaymentNotFoundException(orderId);
                }));
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getOrderId(), p.getAmount(),
                p.getMethod(), p.getStatus(),
                p.getProcessedAt(), p.getCreatedAt()
        );
    }
}