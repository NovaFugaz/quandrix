package com.quandrix.ms_reports.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "Sales Report Response", description = "Respuesta del contenido de un reporte de ventas")
public class SalesReportResponse {
    @Schema(description = "Fecha inicio del reporte", example = "2026-01-01T00:00:00")
    private LocalDateTime from;

    @Schema(description = "Fecha fin del reporte", example = "2026-12-31T00:00:00")
    private LocalDateTime to;

    @Schema(description = "Total de transacciones", example = "150")
    private long totalTransactions;

    @Schema(description = "Monto total de ventas", example = "75000")
    private Long totalAmount;

    @Schema(description = "Monto promedio por transacción", example = "500")
    private Long averageAmount;
}
