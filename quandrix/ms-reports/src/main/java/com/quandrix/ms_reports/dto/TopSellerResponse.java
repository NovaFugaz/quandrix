package com.quandrix.ms_reports.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopSellerResponse {
    @Schema(description = "Id del vendedor", example = "1")
    private Long sellerId;

    @Schema(description = "Total de ventas realizadas por el vendedor", example = "50")
    private long totalSales;

    @Schema(description = "Ingresos totales del vendedor", example = "25000")
    private Long totalRevenue;
}
