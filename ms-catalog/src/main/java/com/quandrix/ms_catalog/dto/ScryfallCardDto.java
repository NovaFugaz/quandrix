package com.quandrix.ms_catalog.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScryfallCardDto {
    private String id;
    private String name;

    @JsonProperty("set")
    private String setCode;

    @JsonProperty("set_name")
    private String setName;

    @JsonProperty("image_uris")
    private ImageUris imageUris;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageUris{
        private String normal;
        private String small;
        private String large;
    }
}
