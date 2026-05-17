package com.quandrix.ms_reports.service;

import com.quandrix.ms_reports.client.TransactionClient;
import com.quandrix.ms_reports.dto.SalesReportResponse;
import com.quandrix.ms_reports.dto.TopCardResponse;
import com.quandrix.ms_reports.dto.TopSellerResponse;
import com.quandrix.ms_reports.dto.TransactionResponse;
import com.quandrix.ms_reports.exception.ReportGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final TransactionClient transactionClient;

    public ReportService(TransactionClient transactionClient) {
        this.transactionClient = transactionClient;
    }

    public SalesReportResponse getSalesReport(LocalDateTime from, LocalDateTime to) {
        log.info("Generando reporte de ventas from={} to={}", from, to);

        List<TransactionResponse> transactions;
        try {
            transactions = transactionClient.getByDateRange(from, to);
        } catch (Exception e) {
            log.error("Error al obtener transacciones: {}", e.getMessage());
            throw new ReportGenerationException(e.getMessage());
        }

        if (transactions.isEmpty()) {
            log.info("Sin transacciones en el período indicado");
            return new SalesReportResponse(from, to, 0, 0L, 0L);
        }

        Long totalAmount = transactions.stream()
                .mapToLong(TransactionResponse::getAmount)
                .sum();

        Long averageAmount = Math.round(
                (double) totalAmount / transactions.size());

        log.info("Reporte generado: {} transacciones, total=${}",
                transactions.size(), totalAmount);
        return new SalesReportResponse(from, to, transactions.size(),
                totalAmount, averageAmount);
    }

    public List<TopSellerResponse> getTopSellers(int limit) {
        log.info("Generando top {} vendedores", limit);

        List<TransactionResponse> transactions;
        try {
            transactions = transactionClient.getAll();
        } catch (Exception e) {
            log.error("Error al obtener transacciones: {}", e.getMessage());
            throw new ReportGenerationException(e.getMessage());
        }

        Map<Long, List<TransactionResponse>> bySeller = transactions.stream()
                .collect(Collectors.groupingBy(TransactionResponse::getSellerId));

        return bySeller.entrySet().stream()
                .map(entry -> {
                    Long sellerId = entry.getKey();
                    List<TransactionResponse> sellerTx = entry.getValue();
                    Long revenue = sellerTx.stream()
                            .mapToLong(TransactionResponse::getAmount)
                            .sum();
                    return new TopSellerResponse(sellerId, sellerTx.size(), revenue);
                })
                .sorted(Comparator.comparingLong(TopSellerResponse::getTotalSales)
                        .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<TopCardResponse> getTopCards(int limit) {
        log.info("Generando top {} cartas más transadas", limit);

        List<TransactionResponse> transactions;
        try {
            transactions = transactionClient.getAll();
        } catch (Exception e) {
            log.error("Error al obtener transacciones: {}", e.getMessage());
            throw new ReportGenerationException(e.getMessage());
        }

        Map<String, Long> byCard = transactions.stream()
                .collect(Collectors.groupingBy(
                        TransactionResponse::getScryfallId,
                        Collectors.counting()));

        return byCard.entrySet().stream()
                .map(entry -> new TopCardResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(TopCardResponse::getTotalSales)
                        .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public SalesReportResponse getGeneralSummary() {
        log.info("Generando resumen general de todas las transacciones");

        List<TransactionResponse> transactions;
        try {
            transactions = transactionClient.getAll();
        } catch (Exception e) {
            log.error("Error al obtener transacciones: {}", e.getMessage());
            throw new ReportGenerationException(e.getMessage());
        }

        if (transactions.isEmpty()) {
            log.info("Sin transacciones registradas aún");
            return new SalesReportResponse(null, null, 0, 0L, 0L);
        }

        Long totalAmount = transactions.stream()
                .mapToLong(TransactionResponse::getAmount)
                .sum();

        Long averageAmount = Math.round(
                (double) totalAmount / transactions.size());

        LocalDateTime oldest = transactions.stream()
                .map(TransactionResponse::getCompletedAt)
                .min(Comparator.naturalOrder()).orElse(null);

        LocalDateTime newest = transactions.stream()
                .map(TransactionResponse::getCompletedAt)
                .max(Comparator.naturalOrder()).orElse(null);

        log.info("Resumen general: {} transacciones, total=${}",
                transactions.size(), totalAmount);
        return new SalesReportResponse(oldest, newest, transactions.size(),
                totalAmount, averageAmount);
    }
}