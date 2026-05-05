package com.quandrix.ms_catalog.service;

import com.quandrix.ms_catalog.client.ScryfallClient;
import com.quandrix.ms_catalog.dto.CardResponse;
import com.quandrix.ms_catalog.dto.ScryfallCardDto;
import com.quandrix.ms_catalog.exception.CardNotFoundException;
import com.quandrix.ms_catalog.model.Card;
import com.quandrix.ms_catalog.model.CardSet;
import com.quandrix.ms_catalog.repository.CardRepository;
import com.quandrix.ms_catalog.repository.CardSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final CardRepository cardRepository;
    private final CardSetRepository cardSetRepository;
    private final ScryfallClient scryfallClient;

    public CatalogService(CardRepository cardRepository,
                          CardSetRepository cardSetRepository,
                          ScryfallClient scryfallClient) {
        this.cardRepository = cardRepository;
        this.cardSetRepository = cardSetRepository;
        this.scryfallClient = scryfallClient;
    }

    public CardResponse getCardById(String scryfallId) {
        return cardRepository.findById(scryfallId)
                .map(card -> {
                    log.info("Carta encontrada localmente: {}", scryfallId);
                    return toResponse(card);
                })
                .orElseGet(() -> {
                    log.info("Carta no encontrada localmente, consultando Scryfall: {}", scryfallId);
                    ScryfallCardDto dto = scryfallClient.getCardById(scryfallId);
                    Card saved = cardRepository.save(toEntity(dto));
                    persistSetIfAbsent(dto);
                    log.info("Carta persistida desde Scryfall: {}", saved.getName());
                    return toResponse(saved);
                });
    }

    public List<CardResponse> searchByName(String name) {
        List<Card> local = cardRepository.searchByName(name);

        if (!local.isEmpty()) {
            log.info("Búsqueda local devolvió {} cartas para '{}'", local.size(), name);
            return local.stream().map(this::toResponse).collect(Collectors.toList());
        }

        log.info("Sin resultados locales para '{}', consultando Scryfall", name);
        List<ScryfallCardDto> dtos = scryfallClient.searchByName(name);

        if (dtos.isEmpty()) {
            throw new CardNotFoundException(name);
        }

        List<Card> saved = dtos.stream()
                .map(dto -> {
                    persistSetIfAbsent(dto);
                    return cardRepository.findById(dto.getId())
                            .orElseGet(() -> cardRepository.save(toEntity(dto)));
                })
                .collect(Collectors.toList());

        log.info("Persistidas {} cartas desde Scryfall para '{}'", saved.size(), name);
        return saved.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<CardResponse> getAllSets() {
        log.info("Obteniendo todos los sets locales");
        return cardRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void persistSetIfAbsent(ScryfallCardDto dto) {
        if (dto.getSetCode() != null &&
                !cardSetRepository.existsById(dto.getSetCode())) {
            CardSet set = new CardSet();
            set.setSetCode(dto.getSetCode());
            set.setSetName(dto.getSetName());
            cardSetRepository.save(set);
            log.info("Set persistido: {} - {}", dto.getSetCode(), dto.getSetName());
        }
    }

    private Card toEntity(ScryfallCardDto dto) {
        Card card = new Card();
        card.setScryfallId(dto.getId());
        card.setName(dto.getName());
        card.setSetCode(dto.getSetCode());
        card.setSetName(dto.getSetName());
        if (dto.getImageUris() != null) {
            card.setImageUrl(dto.getImageUris().getNormal());
        }
        return card;
    }

    private CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getScryfallId(),
                card.getName(),
                card.getSetCode(),
                card.getSetName(),
                card.getImageUrl()
        );
    }
}