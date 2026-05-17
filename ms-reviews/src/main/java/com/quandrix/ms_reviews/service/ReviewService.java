package com.quandrix.ms_reviews.service;

import com.quandrix.ms_reviews.client.NotificationClient;
import com.quandrix.ms_reviews.client.TransactionClient;
import com.quandrix.ms_reviews.dto.*;
import com.quandrix.ms_reviews.exception.*;
import com.quandrix.ms_reviews.model.Review;
import com.quandrix.ms_reviews.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final TransactionClient transactionClient;
    private final NotificationClient notificationClient;

    public ReviewService(ReviewRepository reviewRepository,
                         TransactionClient transactionClient,
                         NotificationClient notificationClient) {
        this.reviewRepository = reviewRepository;
        this.transactionClient = transactionClient;
        this.notificationClient = notificationClient;
    }

    public ReviewResponse create(ReviewRequest request) {
        log.info("Creando reseña de reviewerId={} a sellerId={}",
                request.getReviewerId(), request.getSellerId());

        // Un usuario no puede reseñarse a sí mismo
        if (request.getReviewerId().equals(request.getSellerId())) {
            throw new ReviewNotAllowedException();
        }

        // Verificar que existe una transacción completada entre ambos
        boolean hasTransaction;
        try {
            hasTransaction = transactionClient.existsCompletedTransaction(
                    request.getReviewerId(), request.getSellerId());
        } catch (Exception e) {
            log.error("Error al verificar transacción en ms-transactions: {}",
                    e.getMessage());
            throw new ReviewNotAllowedException();
        }

        if (!hasTransaction) {
            log.warn("reviewerId={} intentó reseñar a sellerId={} sin transacción",
                    request.getReviewerId(), request.getSellerId());
            throw new ReviewNotAllowedException();
        }

        // Verificar que no haya reseñado ya a este vendedor
        if (reviewRepository.existsByReviewerIdAndSellerId(
                request.getReviewerId(), request.getSellerId())) {
            log.warn("Reseña duplicada: reviewerId={} sellerId={}",
                    request.getReviewerId(), request.getSellerId());
            throw new ReviewAlreadyExistsException(
                    request.getReviewerId(), request.getSellerId());
        }

        Review review = new Review();
        review.setReviewerId(request.getReviewerId());
        review.setSellerId(request.getSellerId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        log.info("Reseña creada con id: {} rating: {}", saved.getId(), saved.getRating());

        // Notificar al vendedor
        try {
            notificationClient.send(new NotificationRequest(
                    request.getSellerId(),
                    "REVIEW_RECEIVED",
                    "Recibiste una nueva reseña con calificación " +
                    request.getRating() + "/5"
            ));
        } catch (Exception e) {
            log.error("Error al enviar notificación de reseña: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    public List<ReviewResponse> getBySeller(Long sellerId) {
        log.info("Obteniendo reseñas del vendedor: {}", sellerId);
        return reviewRepository.findBySellerId(sellerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public SellerRatingResponse getSellerRating(Long sellerId) {
        log.info("Calculando rating del vendedor: {}", sellerId);
        Double average = reviewRepository.calculateAverageRating(sellerId)
                .orElse(0.0);
        Long total = reviewRepository.countBySellerId(sellerId);

        // Redondear a 2 decimales
        double rounded = Math.round(average * 100.0) / 100.0;
        log.info("Rating vendedor: {}, promedio: {}, total: {}",
                sellerId, rounded, total);
        return new SellerRatingResponse(sellerId, rounded, total);
    }

    public ReviewResponse getById(Long id) {
        log.info("Buscando reseña id={}", id);
        return toResponse(reviewRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Reseña no encontrada: {}", id);
                    return new ReviewNotFoundException(id);
                }));
    }

    public void delete(Long id) {
        log.info("Eliminando reseña id={}", id);
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException(id));
        reviewRepository.delete(review);
        log.info("Reseña id: {} eliminada", id);
    }

    private ReviewResponse toResponse(Review r) {
        return new ReviewResponse(
                r.getId(), r.getReviewerId(), r.getSellerId(),
                r.getRating(), r.getComment(), r.getCreatedAt()
        );
    }
}