package com.quandrix.ms_reports.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopCardResponse {
    private String scryfallId;
    private long totalSales;
}
