package com.gamecheck.repository;

import com.gamecheck.model.Game;
import com.gamecheck.model.Price;
import com.gamecheck.model.Source;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceRepository extends JpaRepository<Price, Integer> {

    List<Price> findByGame_GameId(Integer gameId);

    @Query("SELECT p FROM Price p JOIN FETCH p.source WHERE p.game.gameId = :gameId")
    List<Price> findByGameIdWithSource(@Param("gameId") Integer gameId);

    @Query("SELECT MIN(p.pricePhp) FROM Price p WHERE p.game.gameId = :gameId")
    Optional<BigDecimal> findMinPricePhpByGame_GameId(@Param("gameId") Integer gameId);

    @Query("SELECT DISTINCT p.source.sourceType FROM Price p WHERE p.game.gameId = :gameId")
    Set<String> findSourceTypesByGame_GameId(@Param("gameId") Integer gameId);

    Optional<Price> findByGameAndSource(Game game, Source source);
}
