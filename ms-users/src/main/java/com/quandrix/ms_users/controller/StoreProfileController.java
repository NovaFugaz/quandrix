package com.quandrix.ms_users.controller;

import com.quandrix.ms_users.dto.StoreProfileRequest;
import com.quandrix.ms_users.dto.StoreProfileResponse;
import com.quandrix.ms_users.service.StoreProfileService;

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
@RequestMapping("/stores")
@Tag(name = "Tiendas", description = "Operaciones relacionadas a los perfiles de tienda")
public class StoreProfileController {

    private final StoreProfileService service;
    private static final Logger log = LoggerFactory.getLogger(StoreProfileController.class);

    public StoreProfileController(StoreProfileService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear perfil de tienda", description = "Crea un nuevo perfil de tienda")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Perfil de tienda creado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProfileResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de perfil inválidos"),
        @ApiResponse(responseCode = "409", description = "El usuario ya tiene perfil de tienda"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<StoreProfileResponse> create(
            @Valid @RequestBody StoreProfileRequest request) {
        log.info("POST /stores userId={} storeName='{}'",
                request.getUserId(), request.getStoreName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Obtener perfil de tienda por usuario", description = "Retorna el perfil de tienda de un usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil de tienda encontrado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontro ese perfil de tienda"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<StoreProfileResponse> getByUserId(
        @Parameter(description = "Id del usuario", required = true, example = "1")
        @PathVariable Long userId) {
        log.info("GET /stores/{}", userId);
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los perfiles de tienda", description = "Retorna todos los perfiles de tienda")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfiles de tienda encontrados con exito",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = StoreProfileResponse.class)))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontraron perfiles de tienda"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<StoreProfileResponse>> getAll() {
        log.info("GET /stores");
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Actualizar perfil de tienda", description = "Actualiza el perfil de tienda")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil de tienda actualizado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreProfileResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos del perfil inválidos"),
        @ApiResponse(responseCode = "404", description = "No se encontro el perfil de tienda"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<StoreProfileResponse> update(
            @Parameter(description = "Id del usuario", required = true, example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody StoreProfileRequest request) {
        log.info("PUT /stores/{}", userId);
        return ResponseEntity.ok(service.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Eliminar perfil de tienda", description = "EElimina un perfil de tienda de un usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Perfil de tienda eliminado con exito"),
        @ApiResponse(responseCode = "404", description = "No se encontro el perfil de tienda"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Id del usuario", required = true, example = "1")
        @PathVariable Long userId) {
        log.info("DELETE /stores/{}", userId);
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }
}