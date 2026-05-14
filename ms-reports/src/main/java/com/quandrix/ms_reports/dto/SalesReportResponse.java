package com.quandrix.ms_reports.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesReportResponse {
    private LocalDateTime from;
    private LocalDateTime to;
    private long totalTransactions;
    private Long totalAmount;
    private Long averageAmount;
}
