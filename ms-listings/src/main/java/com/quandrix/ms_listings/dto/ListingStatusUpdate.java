package com.quandrix.ms_listings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListingStatusUpdate {

    @NotBlank(message = "El status es obligatorio")
    private String status;
}