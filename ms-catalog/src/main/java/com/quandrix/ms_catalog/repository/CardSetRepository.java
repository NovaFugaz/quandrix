package com.quandrix.ms_catalog.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quandrix.ms_catalog.Model.CardSet;

public interface CardSetRepository extends JpaRepository<CardSet, String>{
    
}
