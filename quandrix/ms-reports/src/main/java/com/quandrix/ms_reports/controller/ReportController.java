package com.quandrix.ms_reports.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quandrix.ms_reports.dto.SalesReportResponse;
import com.quandrix.ms_reports.dto.TopCardResponse;
import com.quandrix.ms_reports.dto.TopSellerResponse;
import com.quandrix.ms_reports.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/reports")
@Tag(name = "Reportes", description = "Operaciones relacionadas con reportes")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService){
        this.reportService = reportService;
    }

    @GetMapping("/sales")
    @Operation(summary = "Obtener las ventas", description = "Retorna todas las ventas dentro de un rango de fechas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reporte generado correctamente",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = SalesReportResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Fechas invalidas"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<SalesReportResponse> getSalesReport(
            @Parameter(description = "Fecha de inicio", required = true, example = "2025-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @Parameter(description = "Fecha de fin", required = true, example = "2025-06-13T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        log.info("GET /reports/sales from={} to={}", from, to);
        return ResponseEntity.ok(reportService.getSalesReport(from, to));
    }

    @GetMapping("/top-sellers")
    @Operation(summary = "Obtener top vendedores", description = "Retorna los vendedores con más ventas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top vendedores obtenidos con exito",
            content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = TopSellerResponse.class)))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontraron top vendedores"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<TopSellerResponse>> getTopSellers(
        @Parameter(description = "Cantidad de vendedores", required = true, example = "10")
        @RequestParam(defaultValue = "10") int limit){
            log.info("GET /reports/top-sellers limit={}", limit);
            return ResponseEntity.ok(reportService.getTopSellers(limit));
        }
    

    @GetMapping("/top-cards")
    @Operation(summary = "Obtener cartas más vendidas", description = "Retorna las cartas más vendidas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Top vendedores obtenidos con exito",
            content = @Content(mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = TopCardResponse.class)))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontraron top vendedores"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<TopCardResponse>> getTopCards(
        @RequestParam(defaultValue = "10") int limit){
            log.info("GET /reports/top-cards limit={}", limit);
            return ResponseEntity.ok(reportService.getTopCards(limit));
        }

    @GetMapping("/summary")
    @Operation(summary = "Obtener resumen general", description = "Retorna un resumen general de todas las ventas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resumen obtenido con exito",
            content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = SalesReportResponse.class))
        ),
        @ApiResponse(responseCode = "500",description = "Error interno del servidor")
    })
    public ResponseEntity<SalesReportResponse> getSumary(){
            log.info("GET /reports/summary");
            return ResponseEntity.ok(reportService.getGeneralSummary());
        }
}
