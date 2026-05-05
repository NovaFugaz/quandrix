package com.quandrix.ms_catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CardResponse {
    private String scryfallId;
    private String name;
    private String setCode;
    private String setName;
    private String imageUrl;
}