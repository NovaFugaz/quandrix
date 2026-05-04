package com.quandrix.ms_users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreProfileRequest {

    @NotNull(message = "El userId es obligatorio")
    private Long userId;

    @NotBlank(message = "El nombre de la tienda es obligatorio")
    private String storeName;

    private String location;
    private String description;
}