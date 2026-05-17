package com.quandrix.ms_listings.service;

import com.quandrix.ms_listings.client.CatalogClient;
import com.quandrix.ms_listings.client.UserClient;
import com.quandrix.ms_listings.dto.CardResponse;
import com.quandrix.ms_listings.dto.ListingRequest;
import com.quandrix.ms_listings.dto.ListingResponse;
import com.quandrix.ms_listings.exception.InvalidListingException;
import com.quandrix.ms_listings.exception.ListingNotFoundException;
import com.quandrix.ms_listings.exception.ListingNotAvailableException;
import com.quandrix.ms_listings.model.CardCondition;
import com.quandrix.ms_listings.model.Listing;
import com.quandrix.ms_listings.model.ListingStatus;
import com.quandrix.ms_listings.repository.ListingRepository;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final CatalogClient catalogClient;
    private final UserClient userClient;

    public ListingService(ListingRepository listingRepository,
            CatalogClient catalogClient,
            UserClient userClient) {
        this.listingRepository = listingRepository;
        this.catalogClient = catalogClient;
        this.userClient = userClient;
    }

    @Transactional
    public ListingResponse create(ListingRequest request) {
        log.info("Creando listing para sellerId={} cardName='{}'",
                request.getSellerId(), request.getCardName());

        // 1. Validaciones ANTES de persistir
        try {
            userClient.getUser(request.getSellerId());
        } catch (Exception e) {
            log.warn("Vendedor no encontrado: {}", request.getSellerId());
            throw new InvalidListingException(
                    "El vendedor con id " + request.getSellerId() + " no existe");
        }

        // Resuelve el scryfallId por nombre y set
        CardResponse card;
        try {
            card = catalogClient.findByNameAndSet(
                    request.getCardName(), request.getSetCode());
        } catch (Exception e) {
            log.warn("Carta no encontrada: name='{}' set='{}'",
                    request.getCardName(), request.getSetCode());
            throw new InvalidListingException(
                    "No se encontró la carta '" + request.getCardName() + "'" +
                            (request.getSetCode() != null ? " en el set " + request.getSetCode() : ""));
        }

        log.info("Carta resuelta: {} ({})", card.getName(), card.getScryfallId());

        CardCondition condition;
        try {
            condition = CardCondition.valueOf(request.getCondition().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Condición inválida: {}", request.getCondition());
            throw new InvalidListingException("Condición inválida: " + request.getCondition());
        }

        // 2. Validar precio y cantidad DENTRO de transacción
        if (request.getPrice() < 100 || request.getPrice() > 10_000_000L) {
            log.warn("Precio fuera de rango permitido: {}", request.getPrice());
            throw new InvalidListingException("Precio fuera de rango permitido");
        }

        if (request.getQuantity() < 1 || request.getQuantity() > 100) {
            log.warn("Cantidad fuera de rango permitido: {}", request.getQuantity());
            throw new InvalidListingException("Cantidad debe estar entre 1 y 100");
        }

        // 3. Validar duplicados
        boolean isDuplicate = !listingRepository.findBySellerIdAndScryfallIdAndCardConditionAndStatus(
                request.getSellerId(),
                card.getScryfallId(),
                condition,
                ListingStatus.ACTIVE).isEmpty();

        if (isDuplicate) {
            log.warn("Vendedor ya tiene un listing activo de esta carta");
            throw new InvalidListingException("Ya tienes un listing activo de esta carta");
        }

        // 4. Persistir DENTRO de transacción
        Listing listing = new Listing();
        listing.setSellerId(request.getSellerId());
        listing.setScryfallId(card.getScryfallId());
        listing.setCardCondition(condition);
        listing.setPrice(request.getPrice());
        listing.setQuantity(request.getQuantity());

        Listing saved = listingRepository.save(listing);
        log.info("Listing creado id={} carta='{}' scryfallId={}",
                saved.getId(), card.getName(), saved.getScryfallId());
        return toResponse(saved);
    }

    public ListingResponse getById(Long id) {
        log.info("Buscando listing id={}", id);
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Listing no encontrado: {}", id);
                    return new ListingNotFoundException(id);
                });
        return toResponse(listing);
    }

    public List<ListingResponse> getAllActive() {
        log.info("Obteniendo todos los listings activos");
        return listingRepository.findByStatus(ListingStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ListingResponse> getBySeller(Long sellerId) {
        log.info("Obteniendo listings del seller={}", sellerId);
        return listingRepository.findBySellerIdAndStatus(sellerId, ListingStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<ListingResponse> getByCard(String scryfallId) {
        log.info("Obteniendo listings activos para carta={}", scryfallId);
        return listingRepository.findByScryfallIdAndStatus(scryfallId, ListingStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ListingResponse update(Long id, ListingRequest request) {
        // Se busca el listing a actualizar
        log.info("Actualizando listing id={} por seller={}", id, request.getSellerId());

        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        // No se permite cambiar vendedor
        if (!listing.getSellerId().equals(request.getSellerId())) {
            log.warn("Seller={} intentó actualizar listing que no le pertenece: {}",
                    request.getSellerId(), id);
            throw new InvalidListingException("No puedes cambiar el vendedor");
        }

        if (request.getPrice() < 100 || request.getPrice() > 10_000_000L) {
            log.warn("Precio fuera de rango permitido: {}", request.getPrice());
            throw new InvalidListingException("Precio fuera de rango permitido");
        }

        if (request.getQuantity() < 1 || request.getQuantity() > 100) {
            log.warn("Cantidad fuera de rango permitido: {}", request.getQuantity());
            throw new InvalidListingException("Cantidad fuera de rango permitido");
        }

        CardCondition condition;
        try {
            condition = CardCondition.valueOf(request.getCondition().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Condición inválida: {}", request.getCondition());
            throw new InvalidListingException("Condición inválida: " + request.getCondition());
        }

        listing.setCardCondition(condition);
        listing.setPrice(request.getPrice());
        listing.setQuantity(request.getQuantity());

        Listing updated = listingRepository.save(listing);
        log.info("Listing id={} actualizado exitosamente", id);
        return toResponse(updated);
    }

    public ListingResponse withdraw(Long id, Long sellerId) {
        log.info("Retirando listing id={} por seller={}", id, sellerId);
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        // Solo el dueño del listing puede retirarlo
        if (!listing.getSellerId().equals(sellerId)) {
            log.warn("Seller={} intentó retirar listing que no le pertenece: {}",
                    sellerId, id);
            throw new InvalidListingException(
                    "No puedes retirar un listing que no es tuyo");
        }

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ListingNotAvailableException(id);
        }

        listing.setStatus(ListingStatus.WITHDRAWN);
        Listing updated = listingRepository.save(listing);
        log.info("Listing id={} retirado exitosamente", id);
        return toResponse(updated);
    }

    // Llamado internamente por ms-orders cuando se concreta una compra
    public void markAsSold(Long id) {
        log.info("Marcando listing id={} como SOLD", id);
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ListingNotAvailableException(id);
        }

        // Si el listing tiene más de 1 unidad, se descuenta 1 y se mantiene activo.
        if (listing.getQuantity() > 1) {
            listing.setQuantity(listing.getQuantity() - 1);
            log.info("Listing id={} descontó 1 unidad, quedan {}", id, listing.getQuantity() - 1);
        } else {
            // Última unidad — marca como vendido
            listing.setStatus(ListingStatus.SOLD);
            listing.setQuantity(0);
            log.info("Listing id={} marcado como SOLD (sin stock)", id);
        }
        listingRepository.save(listing);
    }

    public void delete(Long id) {
        log.info("Eliminando listing id={}", id);
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));
        listingRepository.delete(listing);
        log.info("Listing id={} eliminado", id);
    }

    private ListingResponse toResponse(Listing l) {
        return new ListingResponse(
                l.getId(), l.getSellerId(), l.getScryfallId(),
                l.getCardCondition(), l.getPrice(), l.getQuantity(),
                l.getStatus(), l.getCreatedAt());
    }
}