package com.quandrix.ms_listings.repository;

import com.quandrix.ms_listings.model.CardCondition;
import com.quandrix.ms_listings.model.Listing;
import com.quandrix.ms_listings.model.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findBySellerIdAndStatus(Long sellerId, ListingStatus status);

    List<Listing> findByScryfallIdAndStatus(String scryfallId, ListingStatus status);

    List<Listing> findByStatus(ListingStatus status);

    List<Listing> findBySellerIdAndStatusAndCardCondition(
        Long sellerId, ListingStatus status, CardCondition cardCondition);

    List<Listing> findBySellerIdAndScryfallIdAndCardConditionAndStatus(
        Long sellerId, String scryfallId, CardCondition cardCondition, ListingStatus status);
}