package com.quandrix.ms_listings.controller;

import com.quandrix.ms_listings.dto.ListingRequest;
import com.quandrix.ms_listings.dto.ListingResponse;
import com.quandrix.ms_listings.service.ListingService;

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
@RequestMapping("/listings")
@Tag(name = "Publicaciones", description = "Operaciones relacionadas a las publicaciones")
public class ListingController {

    private static final Logger log = LoggerFactory.getLogger(ListingController.class);

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    // POST
    @PostMapping
    @Operation(summary = "Crear una publicación", description = "Crea una publicación para una carta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201",description = "Publicación creada con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ListingResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Publicación con datos inválidos")
    })
    public ResponseEntity<ListingResponse> create(
            @Valid @RequestBody ListingRequest request) {
        log.info("POST /listings - sellerId={}", request.getSellerId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(request));
    }


    // GET - ID
    @GetMapping("/{id}")
    @Operation(summary = "Buscar publicación por Id", description = "Retorna la publicación mediante su Id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publicación encontrada con exito",
            content = @Content(schema = @Schema(implementation = ListingResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se pudo encontrar esa publicación"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ListingResponse> getById(
        @Parameter(description = "Id de la publicación", required = true, example = "1")
        @PathVariable Long id) {
        log.info("GET /listings/{}", id);
        return ResponseEntity.ok(listingService.getById(id));
    }

    // GET - ACTIVOS
    @GetMapping
    @Operation(summary = "Buscar publicaciones activas", description = "Retorna todas las publicaciones activas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publicaciones encontradas con exito", 
        content = @Content(mediaType = "application/json", 
        array = @ArraySchema(schema = @Schema(implementation = ListingResponse.class)))
    ),
    @ApiResponse(responseCode = "404", description = "No se encontraron publicaciones activas")
    })
    public ResponseEntity<List<ListingResponse>> getAllActive() {
        log.info("GET /listings");
        return ResponseEntity.ok(listingService.getAllActive());
    }

    // GET - SELLERID
    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Buscar publicaciones por vendedor", description = "Retorna todas las publicaciones de un vendedor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",description = "Publicaciones encontradas con exito", 
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListingResponse.class)))
        )})
    public ResponseEntity<List<ListingResponse>> getBySeller(
            @Parameter(description = "Id del vendedor", required = true, example = "1")
            @PathVariable Long sellerId) {
        log.info("GET /listings/seller/{}", sellerId);
        return ResponseEntity.ok(listingService.getBySeller(sellerId));
    }

    //GET - SCRYFALLID
    @GetMapping("/card/{scryfallId}")
    @Operation(summary = "Buscar publicaciones por carta", description = "Retorna todas las publicaciones asociadas de una carta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publicaciones encontradas con exito",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListingResponse.class)))
        ),
    @ApiResponse(responseCode = "404",description = "No se encontraron publicaciones de esa carta")
    })
    public ResponseEntity<List<ListingResponse>> getByCard(
            @Parameter(description = "scryfallId de la carta", required = true, example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc")
            @PathVariable String scryfallId) {
        log.info("GET /listings/card/{}", scryfallId);
        return ResponseEntity.ok(listingService.getByCard(scryfallId));
    }

    // PUT - ID
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una publicación", description = "Actualiza los datos de una publicación espeficica")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",description = "Pubkicación actualizada con exito",
            content = @Content(mediaType = "application/json",schema = @Schema(implementation = ListingResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud"),
        @ApiResponse(responseCode = "404",description = "No se pudo encontrar esa publicación"),
        @ApiResponse(responseCode = "500",description = "Error interno del servidor")
    })
    public ResponseEntity<ListingResponse> update(
            @Parameter(description = "Id de la publicación", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ListingRequest request) {
        log.info("PUT /listings/{}", id);
        return ResponseEntity.ok(listingService.update(id, request));
    }

    // PATCH - ID
    @PatchMapping("/{id}/withdraw")
    @Operation(summary = "Retirar una publicación", description = "Marcar una publicación como retirada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Publicación retirada con exito", 
            content = @Content(schema = @Schema(implementation = ListingResponse.class))
        )
    })
    public ResponseEntity<ListingResponse> withdraw(
            @Parameter(description = "Id del vendedor de la publicación", required = true, example = "1")
            @PathVariable Long id,
            @RequestParam Long sellerId) {
        log.info("PATCH /listings/{}/withdraw - sellerId={}", id, sellerId);
        return ResponseEntity.ok(listingService.withdraw(id, sellerId));
    }

    // PATCH - ID
    // Endpoint interno para ms-orders
    @PatchMapping("/{id}/sold")
    @Operation(summary = "Marcar publicación como vendida", description = "Marca una publicación como vendida")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Publicación vendida con exito"),
        @ApiResponse(responseCode = "404", description = "No se pudo encontrar esa publicación"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> markAsSold(
        @Parameter(description = "Id de la publicación", required = true, example = "1")
        @PathVariable Long id) {
        log.info("PATCH /listings/{}/sold", id);
        listingService.markAsSold(id);
        return ResponseEntity.noContent().build();
    }

    // DELETE - ID
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una publicación por Id", description = "Elimina una publicación por su Id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Publicación eliminada correctamente"),
        @ApiResponse(responseCode = "404", description = "No se encontro la publicación"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Id de la publicación a eliminar", required = true, example = "1")
        @PathVariable Long id) {
        log.info("DELETE /listings/{}", id);
        listingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}