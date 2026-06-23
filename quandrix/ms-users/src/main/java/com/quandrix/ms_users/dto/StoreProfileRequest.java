package com.quandrix.ms_users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(title = "Store Profile Request", description = "Datos necesarios para crear/actualizar un perfil de tienda")
public class StoreProfileRequest {

    @NotNull(message = "El userId es obligatorio")
    @Schema(description = "Id de usuario", example = "1")
    private Long userId;

    @NotBlank(message = "El nombre de la tienda es obligatorio")
    @Schema(description = "Nombre de la tienda", example = "Tienda de cartas - MTG")
    private String storeName;

    @Schema(description = "Ubicación de la tienda", example = "Santiago, Av Libertador Bernardo O'higgins - 1122")
    private String location;

    @Schema(description = "Descripción de la tienda", example = "Se venden cartas de MTG")
    private String description;
}