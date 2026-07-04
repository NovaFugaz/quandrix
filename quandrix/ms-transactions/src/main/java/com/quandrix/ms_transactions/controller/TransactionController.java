package com.quandrix.ms_transactions.controller;

import com.quandrix.ms_transactions.dto.TransactionRequest;
import com.quandrix.ms_transactions.dto.TransactionResponse;
import com.quandrix.ms_transactions.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Transacciones", description = "Operaciones relacionadas a las transacciones")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Llamado internamente por ms-orders
    @PostMapping
    @Operation(summary = "Registrar una nueva transacción", description = "Registra una nueva transacción asociada a una orden")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transacción registrada con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de la transacción inválidos"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<TransactionResponse> register(
            @Valid @RequestBody TransactionRequest request) {
        log.info("POST /transactions orderId={}", request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.register(request));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Obtener transacción por orden", description = "Retorna una transacción por su orden")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transacción encontrada con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontro la transacción de esa orden"),
        @ApiResponse(responseCode = "500", description = "Error interno al servidor")
    })
    public ResponseEntity<TransactionResponse> getByOrderId(
            @Parameter(description = "Id de la orden", required = true, example = "1")
            @PathVariable Long orderId) {
        log.info("GET /transactions/order/{}", orderId);
        return ResponseEntity.ok(transactionService.getByOrderId(orderId));
    }

    @GetMapping("/buyer/{buyerId}")
    @Operation(summary = "Obtener transacciones de un comprador", description = "Retorna todas las transacciones de un comprador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transacciones encontradas del comprador con exito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<TransactionResponse>> getByBuyer(
            @Parameter(description = "Id del comprador", required = true, example = "1")
            @PathVariable Long buyerId) {
        log.info("GET /transactions/buyer/{}", buyerId);
        return ResponseEntity.ok(transactionService.getByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Obtener transacciones de un vendedor", description = "Retorna todas las transacciones de un vendedor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transacciones encontradas del vendedor con exito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<TransactionResponse>> getBySeller(
            @Parameter(description = "Id del vendedor", required = true, example = "1")
            @PathVariable Long sellerId) {
        log.info("GET /transactions/seller/{}", sellerId);
        return ResponseEntity.ok(transactionService.getBySeller(sellerId));
    }

    // Usado por ms-reports para generar informes por período
    @GetMapping("/range")
    @Operation(summary = "Obtener transacciones por rango de fechas", description = "Retorna todas las transacciones de un periodo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transacciones de ese periodo encontradas con exito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<TransactionResponse>> getByDateRange(
            @Parameter(description = "Fecha de inicio", required = true, example = "2025-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @Parameter(description = "Fecha de fin", required = true, example = "2026-06-13T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to) {
        log.info("GET /transactions/range from={} to={}", from, to);
        return ResponseEntity.ok(transactionService.getByDateRange(from, to));
    }

    // Solo ADMIN — todas las transacciones
    @GetMapping("/admin")
    @Operation(summary = "Obtener todas las transacciones", description = "Retorna todas las transacciones (ADMINISTRADOR)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transacciones obtenidas con exito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))
        ),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo administrador"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<TransactionResponse>> getAll() {
        log.info("GET /transactions/admin");
        return ResponseEntity.ok(transactionService.getAll());
    }

    // Llamado por ms-reviews para validar transacción previa
    @GetMapping("/exists")
    @Operation(summary = "Verificar si existe transacción", description = "Verifica si existe una transacción completada entre vendedor y comprador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transacción verificada con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = boolean.class))
        ),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Boolean> existsCompletedTransaction(
            @Parameter(description = "Id del comprador", required = true, example = "1")
            @RequestParam Long buyerId,
            @Parameter(description = "Id del vendedor", required = true, example = "1")
            @RequestParam Long sellerId) {
        log.info("GET /transactions/exists buyerId={} sellerId={}", buyerId, sellerId);
        return ResponseEntity.ok(
                transactionService.existsCompletedTransaction(buyerId, sellerId));
    }
}