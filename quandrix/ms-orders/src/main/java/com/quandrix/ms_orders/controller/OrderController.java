package com.quandrix.ms_orders.controller;

import com.quandrix.ms_orders.dto.OrderRequest;
import com.quandrix.ms_orders.dto.OrderResponse;
import com.quandrix.ms_orders.service.OrderService;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Ordenes", description = "Operaciones relacionadas con las ordenes")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Crea una orden", description = "Crea una orden de compra")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Orden creada con exito",
            content = @Content(mediaType = "application/json", 
            schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de orden inválida"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request) {
        log.info("POST /orders buyerId={}", request.getBuyerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una orden por su Id", description = "Retorna la orden especificada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orden encontrada con exito", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontro la notificación"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<OrderResponse> getById(
        @Parameter(description = "Id de la orden", required = true, example = "1")
        @PathVariable Long id) {
        log.info("GET /orders/{}", id);
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping("/buyer/{buyerId}")
    @Operation(summary = "Obtener orden por comprador", description = "Retorna todas las ordenes de compra de un comprador")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ordenes del comprador encontradas con exito",
            content = @Content(mediaType = "application/json", 
            array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<OrderResponse>> getByBuyer(
            @Parameter(description = "Id del comprador", required = true, example = "1")
            @PathVariable Long buyerId) {
        log.info("GET /orders/buyer/{}", buyerId);
        return ResponseEntity.ok(orderService.getByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Obtener orden por vendedor", description = "Retorna todas las ordenes de compra de un vendedor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ordenes del vendedor encontradas con exito",
            content = @Content(mediaType = "application/json", 
            array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<OrderResponse>> getBySeller(
            @Parameter(description = "Id del vendedor", required = true, example = "1")
            @PathVariable Long sellerId) {
        log.info("GET /orders/seller/{}", sellerId);
        return ResponseEntity.ok(orderService.getBySeller(sellerId));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar una orden", description = "Cancelar una orden de compra especifica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orden de compra cancelada con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se pudo encontrar esa orden de compra"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<OrderResponse> cancel(
            @Parameter(description = "Id de la orden de compra", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Id del comprador", required = true, example = "1")
            @RequestParam Long buyerId) {
        log.info("PATCH /orders/{}/cancel buyerId={}", id, buyerId);
        return ResponseEntity.ok(orderService.cancel(id, buyerId));
    }
}