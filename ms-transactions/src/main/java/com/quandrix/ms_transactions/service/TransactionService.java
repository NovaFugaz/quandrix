package com.quandrix.ms_transactions.service;

import com.quandrix.ms_transactions.dto.TransactionRequest;
import com.quandrix.ms_transactions.dto.TransactionResponse;
import com.quandrix.ms_transactions.exception.DuplicateTransactionException;
import com.quandrix.ms_transactions.exception.TransactionNotFoundException;
import com.quandrix.ms_transactions.model.Transaction;
import com.quandrix.ms_transactions.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public TransactionResponse register(TransactionRequest request) {
        log.info("Registrando transacción para orderId={}", request.getOrderId());

        if (transactionRepository.findByOrderId(request.getOrderId()).isPresent()) {
            log.warn("Transacción duplicada para orderId={}", request.getOrderId());
            throw new DuplicateTransactionException(request.getOrderId());
        }

        Transaction transaction = new Transaction();
        transaction.setOrderId(request.getOrderId());
        transaction.setBuyerId(request.getBuyerId());
        transaction.setSellerId(request.getSellerId());
        transaction.setScryfallId(request.getScryfallId());
        transaction.setAmount(request.getAmount());

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transacción registrada con id={}", saved.getId());
        return toResponse(saved);
    }

    public TransactionResponse getByOrderId(Long orderId) {
        log.info("Buscando transacción por orderId={}", orderId);
        return toResponse(transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.warn("Transacción no encontrada para orderId={}", orderId);
                    return new TransactionNotFoundException("orderId: " + orderId);
                }));
    }

    public List<TransactionResponse> getByBuyer(Long buyerId) {
        log.info("Buscando transacciones del comprador={}", buyerId);
        return transactionRepository.findByBuyerId(buyerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getBySeller(Long sellerId) {
        log.info("Buscando transacciones del vendedor={}", sellerId);
        return transactionRepository.findBySellerId(sellerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getByDateRange(
            LocalDateTime from, LocalDateTime to) {
        log.info("Buscando transacciones entre {} y {}", from, to);
        return transactionRepository.findByCompletedAtBetween(from, to)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<TransactionResponse> getAll() {
        log.info("Obteniendo todas las transacciones (admin)");
        return transactionRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // Llamado por ms-reviews para validar que existe transacción entre comprador y vendedor
    public boolean existsCompletedTransaction(Long buyerId, Long sellerId) {
        boolean exists = transactionRepository
                .existsByBuyerIdAndSellerId(buyerId, sellerId);
        log.info("Verificando transacción completada buyerId={} sellerId={}: {}",
                buyerId, sellerId, exists);
        return exists;
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getOrderId(), t.getBuyerId(),
                t.getSellerId(), t.getScryfallId(),
                t.getAmount(), t.getCompletedAt()
        );
    }
}