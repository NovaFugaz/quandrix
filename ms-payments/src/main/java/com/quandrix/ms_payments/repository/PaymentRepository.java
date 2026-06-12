package com.quandrix.ms_payments.repository;

import com.quandrix.ms_payments.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
}