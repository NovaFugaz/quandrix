package com.quandrix.ms_transactions.controller;

import com.quandrix.ms_transactions.dto.TransactionRequest;
import com.quandrix.ms_transactions.dto.TransactionResponse;
import com.quandrix.ms_transactions.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Llamado internamente por ms-orders
    @PostMapping
    public ResponseEntity<TransactionResponse> register(
            @Valid @RequestBody TransactionRequest request) {
        log.info("POST /transactions orderId={}", request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.register(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<TransactionResponse> getByOrderId(
            @PathVariable Long orderId) {
        log.info("GET /transactions/order/{}", orderId);
        return ResponseEntity.ok(transactionService.getByOrderId(orderId));
    }

    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<List<TransactionResponse>> getByBuyer(
            @PathVariable Long buyerId) {
        log.info("GET /transactions/buyer/{}", buyerId);
        return ResponseEntity.ok(transactionService.getByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<TransactionResponse>> getBySeller(
            @PathVariable Long sellerId) {
        log.info("GET /transactions/seller/{}", sellerId);
        return ResponseEntity.ok(transactionService.getBySeller(sellerId));
    }

    // Usado por ms-reports para generar informes por período
    @GetMapping("/range")
    public ResponseEntity<List<TransactionResponse>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        log.info("GET /transactions/range from={} to={}", from, to);
        return ResponseEntity.ok(transactionService.getByDateRange(from, to));
    }

    // Solo ADMIN — todas las transacciones
    @GetMapping("/admin")
    public ResponseEntity<List<TransactionResponse>> getAll() {
        log.info("GET /transactions/admin");
        return ResponseEntity.ok(transactionService.getAll());
    }

    // Llamado por ms-reviews para validar transacción previa
    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsCompletedTransaction(
            @RequestParam Long buyerId,
            @RequestParam Long sellerId) {
        log.info("GET /transactions/exists buyerId={} sellerId={}", buyerId, sellerId);
        return ResponseEntity.ok(
                transactionService.existsCompletedTransaction(buyerId, sellerId));
    }
}