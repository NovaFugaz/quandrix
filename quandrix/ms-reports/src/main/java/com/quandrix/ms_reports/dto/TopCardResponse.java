package com.quandrix.ms_reports.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(title = "Top Card Response", description = "Respuesta del contenido del reporte de cartas más vendidas")
public class TopCardResponse {
    @Schema(description = "Id único de carta", example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc")
    private String scryfallId;

    @Schema(description = "Ventas totales", example = "150")
    private long totalSales;
}
