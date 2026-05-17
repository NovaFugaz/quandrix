package com.quandrix.ms_payments.service;

import com.quandrix.ms_payments.dto.PaymentRequest;
import com.quandrix.ms_payments.dto.PaymentResponse;
import com.quandrix.ms_payments.exception.PaymentNotFoundException;
import com.quandrix.ms_payments.exception.PaymentProcessingException;
import com.quandrix.ms_payments.model.Payment;
import com.quandrix.ms_payments.model.PaymentMethod;
import com.quandrix.ms_payments.model.PaymentStatus;
import com.quandrix.ms_payments.repository.PaymentRepository;

import jakarta.transaction.Transactional;

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

    @Transactional
    public PaymentResponse process(PaymentRequest request) {
        log.info("Procesando pago para orderId={} monto={}",
                request.getOrderId(), request.getAmount());

        // Validar que no exista pago anterior
        if (paymentRepository.existsByOrderId(request.getOrderId())) {
            log.warn("Ya existe un pago para orderId={}", request.getOrderId());
            throw new PaymentProcessingException(
                    "Ya existe un pago registrado para la orden " + request.getOrderId());
        }

        if (request.getAmount() <= 0) {
        log.warn("Monto inválido para orderId={}: {}", request.getOrderId(), request.getAmount());
        throw new PaymentProcessingException("Monto debe ser positivo");
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

        if (Boolean.TRUE.equals(request.getForceFailure())) {
            // Opción explícita para testing y para la defensa.
            payment.setStatus(PaymentStatus.REJECTED);
            log.warn("Pago rechazado de forma simulada (forceFailure=true) para orderId={}", 
                     request.getOrderId());
        } else {
            // Opción aleatoria 10% para simular fallos reales
            boolean shouldReject = System.currentTimeMillis() % 10 == 0;
            payment.setStatus(shouldReject ? PaymentStatus.REJECTED : PaymentStatus.APPROVED);
            
            if (shouldReject) {
                log.warn("Pago rechazado aleatoriamente (10%) para orderId={}", 
                 request.getOrderId());
            }
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            payment.setProcessedAt(LocalDateTime.now());
        }

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