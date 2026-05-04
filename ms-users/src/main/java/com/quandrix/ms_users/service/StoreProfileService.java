package com.quandrix.ms_users.service;

import com.quandrix.ms_users.dto.StoreProfileRequest;
import com.quandrix.ms_users.dto.StoreProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.model.StoreProfile;
import com.quandrix.ms_users.repository.StoreProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreProfileService {

    private final StoreProfileRepository repository;

    public StoreProfileService(StoreProfileRepository repository) {
        this.repository = repository;
    }

    public StoreProfileResponse create(StoreProfileRequest request) {
        if (repository.existsByUserId(request.getUserId())) {
            throw new ProfileAlreadyExistsException(request.getUserId());
        }
        StoreProfile profile = new StoreProfile();
        profile.setUserId(request.getUserId());
        profile.setStoreName(request.getStoreName());
        return toResponse(repository.save(profile));
    }

    public StoreProfileResponse getByUserId(Long userId) {
        return toResponse(repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)));
    }

    public List<StoreProfileResponse> getAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public StoreProfileResponse update(Long userId, StoreProfileRequest request) {
        StoreProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        profile.setStoreName(request.getStoreName());
        return toResponse(repository.save(profile));
    }

    public void delete(Long userId) {
        StoreProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        repository.delete(profile);
    }

    private StoreProfileResponse toResponse(StoreProfile s) {
        return new StoreProfileResponse(
                s.getId(), s.getUserId(), s.getStoreName(),
                s.getCreatedAt()
        );
    }
}