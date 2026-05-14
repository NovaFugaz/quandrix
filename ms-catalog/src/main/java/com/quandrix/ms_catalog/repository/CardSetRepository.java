package com.quandrix.ms_catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quandrix.ms_catalog.model.CardSet;

public interface CardSetRepository extends JpaRepository<CardSet, String>{
    
}
