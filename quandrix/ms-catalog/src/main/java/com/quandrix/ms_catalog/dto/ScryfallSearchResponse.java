package com.quandrix.ms_catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScryfallSearchResponse {

    private List<ScryfallCardDto> data;

    @JsonProperty("has_more")
    private boolean hasMore;

    @JsonProperty("total_cards")
    private int totalCards;
}