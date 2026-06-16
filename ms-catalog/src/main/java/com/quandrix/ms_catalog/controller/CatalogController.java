package com.quandrix.ms_catalog.controller;

import com.quandrix.ms_catalog.dto.CardResponse;
import com.quandrix.ms_catalog.service.CatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
@Tag(name = "Catalogo", description = "Operaciones relacionadas con el catalogo")
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{scryfallId}")
    @Operation(summary = "Obtener carta por ID", description = "Retorna una carta usando su scryfallId")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carta encontrada exitosamente", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "Carta no encontrada con ese ID"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor") })
    public ResponseEntity<CardResponse> getById(
            @Parameter(description = "Id único de Scryfall de la carta", required = true, example = "bd8fa8c8-7e1c-4f5d-a6d3-123456789abc") 
            @PathVariable String scryfallId) {
        log.info("GET /catalog/{}", scryfallId);
        return ResponseEntity.ok(catalogService.getCardById(scryfallId));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar cartas por nombre", description = "Retorna las cartas que coincidan con el nombre ingresado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda exitosa", 
                content = @Content(mediaType = "application/json", 
                array = @ArraySchema(schema = @Schema(implementation = CardResponse.class)))),
            @ApiResponse(responseCode = "400", description = "El nombre de búsqueda es inválido o está vacío"),
            @ApiResponse(responseCode = "404", description = "Carta no encontrada con ese nombre"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<CardResponse>> search(
            @Parameter(description = "Nombre de la carta a buscar", required = true, example = "Black Lotus") 
            @RequestParam String name) {
        log.info("GET /catalog/search?name={}", name);
        return ResponseEntity.ok(catalogService.searchByName(name));
    }

    @GetMapping("/sets")
    @Operation(summary = "Obtener todos los sets", description = "Retorna todos los sets disponibles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sets obtenidos exitosamente", 
                content = @Content(mediaType = "application/json", 
                array = @ArraySchema(schema = @Schema(implementation = CardResponse.class)))),
            @ApiResponse(responseCode = "404", description = "No se encontraron sets"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<CardResponse>> getAllSets() {
        log.info("GET /catalog/sets");
        return ResponseEntity.ok(catalogService.getAllSets());
    }

    @GetMapping("/find")
    @Operation(summary = "Buscar carta por su nombre y código de set", description = "Permite buscar una carta, filtrando por su nombre y código del set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carta encontrada", 
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CardResponse.class))),
            @ApiResponse(responseCode = "404", description = "No se encontró una carta con los filtros indicados"),
            @ApiResponse(responseCode = "400", description = "Parámetros de búsqueda inválidos"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<CardResponse> findByNameAndSet(
            @Parameter(description = "Nombre de la carta", required = true, example = "Black Lotus") 
            @RequestParam String name,
            @Parameter(description = "Código del set (opcional)", example = "LEA") 
            @RequestParam(required = false) String setCode) {
        log.info("GET /catalog/find name={} setCode={}", name, setCode);
        return ResponseEntity.ok(catalogService.findByNameAndSet(name, setCode));
    }
}