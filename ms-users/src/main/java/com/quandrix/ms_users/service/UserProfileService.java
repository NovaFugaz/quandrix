package com.quandrix.ms_users.service;

import com.quandrix.ms_users.dto.UserProfileRequest;
import com.quandrix.ms_users.dto.UserProfileResponse;
import com.quandrix.ms_users.exception.ProfileAlreadyExistsException;
import com.quandrix.ms_users.exception.UserNotFoundException;
import com.quandrix.ms_users.model.UserProfile;
import com.quandrix.ms_users.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfileResponse create(UserProfileRequest request) {
        if (repository.existsByUserId(request.getUserId())) {
            throw new ProfileAlreadyExistsException(request.getUserId());
        }
        UserProfile profile = new UserProfile();
        profile.setUserId(request.getUserId());
        profile.setDisplayName(request.getDisplayName());
        return toResponse(repository.save(profile));
    }

    public UserProfileResponse getByUserId(Long userId) {
        return toResponse(repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)));
    }

    public List<UserProfileResponse> getAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserProfileResponse update(Long userId, UserProfileRequest request) {
        UserProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        profile.setDisplayName(request.getDisplayName());
        return toResponse(repository.save(profile));
    }

    public void delete(Long userId) {
        UserProfile profile = repository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        repository.delete(profile);
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return new UserProfileResponse(
                p.getId(), p.getUserId(), p.getDisplayName(),
                p.getCreatedAt()
        );
    }
}