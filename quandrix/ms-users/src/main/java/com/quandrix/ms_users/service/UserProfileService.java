package com.quandrix.ms_users.service;

import com.quandrix.ms_users.dto.UserProfileRequest;
import com.quandrix.ms_users.dto.UserProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.model.UserProfile;
import com.quandrix.ms_users.repository.UserProfileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

        private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

public UserProfileResponse create(UserProfileRequest request) {
        log.info("Creando perfil de usuario para userId={}", request.getUserId());

        if (repository.existsByUserId(request.getUserId())) {
            log.warn("Ya existe un perfil para userId={}", request.getUserId());
            throw new ProfileAlreadyExistsException(request.getUserId());
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(request.getUserId());
        profile.setDisplayName(request.getDisplayName());

        UserProfile saved = repository.save(profile);
        log.info("Perfil creado exitosamente con id={} para userId={}",
                saved.getId(), saved.getUserId());
        return toResponse(saved);
    }

    public UserProfileResponse getByUserId(Long userId) {
        log.info("Buscando perfil para userId={}", userId);

        return repository.findByUserId(userId)
                .map(profile -> {
                    log.info("Perfil encontrado para userId={}", userId);
                    return toResponse(profile);
                })
                .orElseThrow(() -> {
                    log.warn("Perfil no encontrado para userId={}", userId);
                    return new UserNotFoundException(userId);
                });
    }

    public List<UserProfileResponse> getAll() {
        log.info("Obteniendo todos los perfiles de usuario");
        List<UserProfileResponse> profiles = repository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.info("Se encontraron {} perfiles", profiles.size());
        return profiles;
    }

    public UserProfileResponse update(Long userId, UserProfileRequest request) {
        log.info("Actualizando perfil para userId={}", userId);

        UserProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Perfil no encontrado para actualizar userId={}", userId);
                    return new UserNotFoundException(userId);
                });

        profile.setDisplayName(request.getDisplayName());

        UserProfile updated = repository.save(profile);
        log.info("Perfil actualizado exitosamente para userId={}", userId);
        return toResponse(updated);
    }

    public void delete(Long userId) {
        log.info("Eliminando perfil para userId={}", userId);

        UserProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.warn("Perfil no encontrado para eliminar userId={}", userId);
                    return new UserNotFoundException(userId);
                });

        repository.delete(profile);
        log.info("Perfil eliminado exitosamente para userId={}", userId);
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return new UserProfileResponse(
                p.getId(), p.getUserId(), p.getDisplayName(),
                p.getCreatedAt()
        );
    }
}