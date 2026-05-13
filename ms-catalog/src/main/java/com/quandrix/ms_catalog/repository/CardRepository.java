package com.quandrix.ms_catalog.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quandrix.ms_catalog.Model.Card;

@Repository
public interface CardRepository extends JpaRepository<Card, String>{
    
    List<Card> searchByName(@Param("name") String name);
}
