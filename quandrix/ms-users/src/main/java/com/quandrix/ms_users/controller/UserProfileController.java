package com.quandrix.ms_users.controller;

import com.quandrix.ms_users.dto.UserProfileRequest;
import com.quandrix.ms_users.dto.UserProfileResponse;
import com.quandrix.ms_users.service.UserProfileService;

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
@RequestMapping("/users")
@Tag(name = "Usuarios", description = "Operaciones relacionadas con los usuarios")
public class UserProfileController {

    private final UserProfileService service;
    private static final Logger log = LoggerFactory.getLogger(UserProfileController.class);

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear perfil de usuario", description = "Crea un nuevo perfil de usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Perfil de usuario creado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos de perfil inválidos"),
        @ApiResponse(responseCode = "409", description = "El usuario ya tiene un perfil"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<UserProfileResponse> create(
            @Valid @RequestBody UserProfileRequest request) {
        log.info("POST /users userId={}", request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Obtener perfil de usuario", description = "Retorna el perfil de un usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil de usuario encontrado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "No se encontro ese perfil de usuario"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<UserProfileResponse> getByUserId(
        @Parameter(description = "Id del usuario", required = true, example = "1")
        @PathVariable Long userId) {
        log.info("GET /users/{}", userId);
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @GetMapping
    @Operation(summary = "Obtener todos los perfiles de usuario", description = "Retorna todos los perfiles de usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfiles de usuarios encontrados con exito",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = UserProfileResponse.class)))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<List<UserProfileResponse>> getAll() {
        log.info("GET /users");
        return ResponseEntity.ok(service.getAll());
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Actualizar perfil de usuario", description = "Actualiza el perfil de usuario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Perfil de usuario actualizado con exito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Datos del perfil inválidos"),
        @ApiResponse(responseCode = "404", description = "No se encontro el perfil de usuario"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<UserProfileResponse> update(
            @Parameter(description = "Id del usuario", required = true, example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileRequest request) {
        log.info("PUT /users/{}", userId);
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
        log.info("DELETE /users/{}", userId);
        service.delete(userId);
        return ResponseEntity.noContent().build();
    }
}