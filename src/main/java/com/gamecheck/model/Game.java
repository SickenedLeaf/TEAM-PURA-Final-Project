package com.gamecheck.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Integer gameId;

    @Column(name = "game_title", nullable = false)
    private String gameTitle;

    @Column(name = "product_code", length = 8, unique = true)
    private String productCode;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(name = "metacritic_score")
    private Integer metacriticScore;

    @Column(name = "trailer_url", columnDefinition = "TEXT")
    private String trailerUrl;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;
}
