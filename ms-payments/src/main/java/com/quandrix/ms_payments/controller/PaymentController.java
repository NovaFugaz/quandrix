package com.quandrix.ms_payments.controller;

import com.quandrix.ms_payments.dto.PaymentRequest;
import com.quandrix.ms_payments.dto.PaymentResponse;
import com.quandrix.ms_payments.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@Tag(name = "Pagos", description = "Operaciones relacianas a los pagos")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/process")
    @Operation(summary = "Procesamiento de un pago", description = "Procesa el pago asociado a una orden")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Pago procesado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos del pago inválidos"),
        @ApiResponse(responseCode = "404", description = "No se encontro la orden asociada"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<PaymentResponse> process(
            @Valid @RequestBody PaymentRequest request) {
        log.info("POST /payments/process orderId={}", request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.process(request));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Obtener pago por orden", description = "Retorna el pago asociado a una orden")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pago encontrado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontró un pago para esa orden"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<PaymentResponse> getByOrderId(
            @Parameter(description = "Id de la orden", required = true, example = "1")
            @PathVariable Long orderId) {
        log.info("GET /payments/order/{}", orderId);
        return ResponseEntity.ok(paymentService.getByOrderId(orderId));
    }
}