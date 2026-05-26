package com.gamecheck.repository;

import com.gamecheck.model.Game;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Integer> {

  @Query(
      value =
          """
          SELECT game_id, game_title, platform, metacritic_score, trailer_url, cover_image_url, product_code
          FROM games
          WHERE search_vector @@ plainto_tsquery('english', :query)
          """,
      nativeQuery = true)
  List<Game> searchByFullText(@Param("query") String query);

  @Query(
      value =
          """
          SELECT game_id, game_title, platform, metacritic_score, trailer_url, cover_image_url, product_code
          FROM games
          WHERE search_vector @@ plainto_tsquery('english', :query)
            AND platform = :platform
          """,
      nativeQuery = true)
  List<Game> searchByFullTextAndPlatform(@Param("query") String query, @Param("platform") String platform);

  List<Game> findByPlatform(String platform);

  Optional<Game> findByProductCode(String productCode);
}
