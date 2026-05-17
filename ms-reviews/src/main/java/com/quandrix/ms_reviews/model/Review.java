package com.quandrix.ms_reviews.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/* A futuro deberíamos implementar acá el campo orderId y cambiar la validación
Así podríamos permitir que un usuario reseñe varias veces al mismo vendedor, pero
por órdenes y listings diferentes. No es importante ahora, pero estaría bien a futuro */ 


@Getter
@Setter
@Entity
@Table(name = "reviews", uniqueConstraints = 
    @UniqueConstraint(columnNames = {"reviewer_id", "seller_id"}))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, name = "reviewer_id") // ref. externas
    private Long reviewerId;
    
    @Column(nullable = false, name = "seller_id") // ref. externas
    private Long sellerId;
    
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;
    
    @Column(length = 500)
    private String comment;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
