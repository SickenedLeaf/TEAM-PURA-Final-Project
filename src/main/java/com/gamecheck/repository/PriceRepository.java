package com.gamecheck.repository;

import com.gamecheck.model.Price;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceRepository extends JpaRepository<Price, Integer> {

    List<Price> findByGame_GameId(Integer gameId);

    @Query("SELECT p FROM Price p JOIN FETCH p.source WHERE p.game.gameId = :gameId")
    List<Price> findByGameIdWithSource(@Param("gameId") Integer gameId);

    @Query("SELECT MIN(p.pricePhp) FROM Price p WHERE p.game.gameId = :gameId")
    Optional<BigDecimal> findMinPricePhpByGame_GameId(@Param("gameId") Integer gameId);
}
