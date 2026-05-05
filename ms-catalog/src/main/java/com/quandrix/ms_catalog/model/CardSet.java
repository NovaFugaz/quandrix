package com.quandrix.ms_catalog.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_sets")
public class CardSet {

    @Id
    private String setCode;
    
    @Column(nullable = false)
    private String setName;

    @OneToMany(mappedBy = "cardSet", fetch = FetchType.LAZY)
    private List<Card> cards;

}
