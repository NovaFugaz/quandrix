package com.quandrix.ms_catalog.client;

import com.quandrix.ms_catalog.dto.ScryfallCardDto;
import com.quandrix.ms_catalog.dto.ScryfallSearchResponse;
import com.quandrix.ms_catalog.exception.CardNotFoundException;
import com.quandrix.ms_catalog.exception.ScryfallApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
public class ScryfallClient {

    private static final Logger log = LoggerFactory.getLogger(ScryfallClient.class);

    private final WebClient webClient;

    public ScryfallClient(WebClient scryfallWebClient) {
        this.webClient = scryfallWebClient;
    }

    public ScryfallCardDto getCardById(String scryfallId) {
        log.info("Consultando Scryfall por ID: {}", scryfallId);
        try {
            return webClient.get()
                    .uri("/cards/{id}", scryfallId)
                    .retrieve()
                    .bodyToMono(ScryfallCardDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Carta no encontrada en Scryfall con ID: {}", scryfallId);
            throw new CardNotFoundException(scryfallId);
        } catch (Exception e) {
            log.error("Error al consultar Scryfall por ID {}: {}", scryfallId, e.getMessage());
            throw new ScryfallApiException(e.getMessage());
        }
    }

    public List<ScryfallCardDto> searchByName(String name) {
        log.info("Consultando Scryfall por nombre: {}", name);
        try {
            ScryfallSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards/search")
                            .queryParam("q", "name:" + name)
                            .queryParam("unique", "prints")
                            .build())
                    .retrieve()
                    .bodyToMono(ScryfallSearchResponse.class)
                    .block();

            if (response == null || response.getData() == null) {
                log.warn("Scryfall no devolvió resultados para: {}", name);
                return List.of();
            }

            log.info("Scryfall devolvió {} cartas para '{}'", response.getData().size(), name);
            return response.getData();

        } catch (WebClientResponseException.NotFound e) {
            log.warn("Ninguna carta encontrada en Scryfall para nombre: {}", name);
            return List.of();
        } catch (Exception e) {
            log.error("Error al buscar en Scryfall por nombre {}: {}", name, e.getMessage());
            throw new ScryfallApiException(e.getMessage());
        }
    }
}