package com.quandrix.ms_reports.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesReportResponse {
    private LocalDateTime from;
    private LocalDateTime to;
    private long totalTransactions;
    private BigDecimal totalAmount;
    private BigDecimal averageAmount;
}
