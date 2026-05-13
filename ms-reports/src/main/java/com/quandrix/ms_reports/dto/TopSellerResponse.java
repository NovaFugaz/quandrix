package com.quandrix.ms_reports.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopSellerResponse {
    private Long sellerId;
    private long totalSales;
    private BigDecimal totalRevenue;
}
