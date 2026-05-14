package com.quandrix.ms_catalog.controller;

import com.quandrix.ms_catalog.dto.CardResponse;
import com.quandrix.ms_catalog.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/{scryfallId}")
    public ResponseEntity<CardResponse> getById(@PathVariable String scryfallId) {
        log.info("GET /catalog/{}", scryfallId);
        return ResponseEntity.ok(catalogService.getCardById(scryfallId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CardResponse>> search(@RequestParam String name) {
        log.info("GET /catalog/search?name={}", name);
        return ResponseEntity.ok(catalogService.searchByName(name));
    }

    @GetMapping("/sets")
    public ResponseEntity<List<CardResponse>> getAllSets() {
        log.info("GET /catalog/sets");
        return ResponseEntity.ok(catalogService.getAllSets());
    }
}