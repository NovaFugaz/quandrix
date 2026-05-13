package com.quandrix.ms_reviews.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.quandrix.ms_reviews.model.Review;

import feign.Param;

@Repository
public interface ReviewRepository extends JpaRepository <Review, Long>{

    List<Review> findBySellerId(Long sellerId);
    
    Optional<Review> findByReviewerIdAndSellerId(Long reviewerId, Long sellerId);
    
    boolean existsByReviewerIdAndSellerId(Long reviewerId, Long sellerId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.sellerId = :sellerId")
    Optional<Double> calculateAverageRating(@Param("sellerId") Long sellerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.sellerId = :sellerId")
    Long countBySellerId(@Param("sellerId") Long sellerId);
}
