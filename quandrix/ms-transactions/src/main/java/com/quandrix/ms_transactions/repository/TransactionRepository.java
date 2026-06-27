package com.quandrix.ms_transactions.repository;

import com.quandrix.ms_transactions.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByOrderId(Long orderId);

    List<Transaction> findByBuyerId(Long buyerId);

    List<Transaction> findBySellerId(Long sellerId);

    List<Transaction> findByCompletedAtBetween(
            LocalDateTime from, LocalDateTime to);

    boolean existsByBuyerIdAndSellerId(Long buyerId, Long sellerId);

    List<Transaction> findByScryfallId(String scryfallId);
}