package com.quandrix.ms_users.service;

import com.quandrix.ms_users.dto.StoreProfileRequest;
import com.quandrix.ms_users.dto.StoreProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.model.StoreProfile;
import com.quandrix.ms_users.repository.StoreProfileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreProfileService {

    private static final Logger log = LoggerFactory.getLogger(StoreProfileService.class);
    private final StoreProfileRepository repository;

    public StoreProfileService(StoreProfileRepository repository) {
        this.repository = repository;
    }

    public StoreProfileResponse create(StoreProfileRequest request) {
        log.info("Creando perfil de tienda para userId={} storeName='{}'",
                request.getUserId(), request.getStoreName());

        if (repository.existsByUserId(request.getUserId())) {
            log.warn("Ya existe un perfil de tienda para userId={}", request.getUserId());
            throw new ProfileAlreadyExistsException(request.getUserId());
        }

        StoreProfile profile = new StoreProfile();
        profile.setUserId(request.getUserId());
        profile.setStoreName(request.getStoreName());
        
        StoreProfile saved = repository.save(profile);
        log.info("Perfil de tienda creado con id={} para userId={}",
        saved.getId(), saved.getUserId());
        return toResponse(saved);
    }


    public StoreProfileResponse getByUserId(Long userId) {
        log.info("Buscando perfil de tienda para userId={}", userId);

        return repository.findByUserId(userId)
                .map(profile -> {
                    log.info("Perfil de tienda encontrado para userId={}", userId);
                    return toResponse(profile);
                })
                .orElseThrow(() -> {
                    log.warn("Perfil de tienda no encontrado para userId={}", userId);
                    return new UserNotFoundException(userId);
                });
    }

    public List<StoreProfileResponse> getAll() {
        log.info("Obteniendo todos los perfiles de tienda");
        List<StoreProfileResponse> stores = repository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.info("Se encontraron {} tiendas", stores.size());
        return stores;
    }

    public StoreProfileResponse update(Long userId, StoreProfileRequest request) {
        log.info("Actualizando perfil de tienda para userId={}", userId);

        StoreProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Perfil de tienda no encontrado para actualizar userId={}",
                            userId);
                    return new UserNotFoundException(userId);
                });

        profile.setStoreName(request.getStoreName());
        StoreProfile updated = repository.save(profile);
        log.info("Perfil de tienda actualizado exitosamente para userId={}", userId);
        return toResponse(updated);
    }


    public void delete(Long userId) {
        log.info("Eliminando perfil de tienda para userId={}", userId);

        StoreProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Perfil de tienda no encontrado para eliminar userId={}",
                            userId);
                    return new UserNotFoundException(userId);
                });

        repository.delete(profile);
        log.info("Perfil de tienda eliminado exitosamente para userId={}", userId);
    }

    private StoreProfileResponse toResponse(StoreProfile s) {
        return new StoreProfileResponse(
                s.getId(), s.getUserId(), s.getStoreName(),
                s.getCreatedAt()
        );
    }
}