package com.gamecheck.repository;

import com.gamecheck.model.Price;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRepository extends JpaRepository<Price, Integer> {

    List<Price> findByGame_GameId(Integer gameId);
}
