package com.gamecheck.repository;

import com.gamecheck.model.Game;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Integer> {

    List<Game> findByGameTitleContainingIgnoreCase(String title);

    List<Game> findByPlatform(String platform);

    List<Game> findByGameTitleContainingIgnoreCaseAndPlatform(String title, String platform);
}
