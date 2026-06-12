package com.quandrix.ms_catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.quandrix.ms_catalog.model.Card;

public interface CardRepository extends JpaRepository<Card, String>{
    
    List<Card> searchByName(@Param("name") String name);
}
