package com.quandrix.ms_users.repository;

import com.quandrix.ms_users.model.StoreProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreProfileRepository extends JpaRepository<StoreProfile, Long> {
    Optional<StoreProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}