package com.quandrix.ms_listings.repository;

import com.quandrix.ms_listings.model.CardCondition;
import com.quandrix.ms_listings.model.Listing;
import com.quandrix.ms_listings.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findBySellerIdAndStatus(Long sellerId, ListingStatus status);

    List<Listing> findByScryfallIdAndStatus(String scryfallId, ListingStatus status);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findBySellerIdAndStatusAndCondition(
        Long sellerId, ListingStatus status, CardCondition condition);
}