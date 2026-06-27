package com.quandrix.ms_reviews.controller;

import com.quandrix.ms_reviews.dto.ReviewRequest;
import com.quandrix.ms_reviews.dto.ReviewResponse;
import com.quandrix.ms_reviews.dto.SellerRatingResponse;
import com.quandrix.ms_reviews.service.ReviewService;

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
@RequestMapping("/reviews")
@Tag(name = "Reseñas", description = "Operaciones relacionadas a las reseñas")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @Operation(summary = "Crear una reseña", description = "Crear una nueva reseña")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Reseña creada con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos inválidos para la reseña"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody ReviewRequest request) {
        log.info("POST /reviews reviewerId={} sellerId={}",
                request.getReviewerId(), request.getSellerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    @GetMapping("/seller/{sellerId}")
    @Operation(summary = "Obtener las reseñas de un vendedor", description = "Retorna todas las reseñas de un vendedor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reseñas de vendedor encontradas con exito",
            content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ReviewResponse.class)))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontro ninguna reseña para ese vendedor"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<ReviewResponse>> getBySeller(
            @Parameter(description = "Id del vendedor", required = true, example = "1")
            @PathVariable Long sellerId) {
        log.info("GET /reviews/seller/{}", sellerId);
        return ResponseEntity.ok(reviewService.getBySeller(sellerId));
    }

    @GetMapping("/seller/{sellerId}/rating")
    @Operation(summary = "Obtener la calificación de un vendedor", description = "Retorna la calificación de un vendedor")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calificación encontrada con exito", 
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = SellerRatingResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontro calificación para ese vendedor"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<SellerRatingResponse> getSellerRating(
            @Parameter(description = "Id del vendedor", required = true, example = "1")
            @PathVariable Long sellerId) {
        log.info("GET /reviews/seller/{}/rating", sellerId);
        return ResponseEntity.ok(reviewService.getSellerRating(sellerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener reseña por Id", description = "Retorna una reseña especificada")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reseña obtenida con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReviewResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se pudo encontrar esa reseña"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<ReviewResponse> getById(
        @Parameter(description = "Id de la reseña", required = true, example = "1")
        @PathVariable Long id) {
        log.info("GET /reviews/{}", id);
        return ResponseEntity.ok(reviewService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una reseña", description = "Eliminar reseña por su Id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Reseña eliminada con exito"),
        @ApiResponse(responseCode = "404", description = "No se pudo encontrar la reseña"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Id de reseña", required = true, example = "1")
        @PathVariable Long id) {
        log.info("DELETE /reviews/{}", id);
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}