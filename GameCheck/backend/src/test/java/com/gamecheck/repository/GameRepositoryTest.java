package com.gamecheck.repository;

import com.gamecheck.model.Game;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(excludeAutoConfiguration = {
    FlywayAutoConfiguration.class,
    LiquibaseAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class GameRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    @Test
    void testSaveAndFindGame_Success() {
        // Arrange
        Game game = Game.builder()
                .gameTitle("The Legend of Zelda")
                .platform("Nintendo Switch")
                .productCode("ZELDA01")
                .metacriticScore(97)
                .trailerUrl("https://youtube.com/watch?v=zelda")
                .coverImageUrl("https://example.com/zelda.jpg")
                .build();

        // Act - Save the game
        Game savedGame = gameRepository.save(game);
        entityManager.flush();
        entityManager.clear();

        // Assert - Find by ID
        assertTrue(gameRepository.findById(savedGame.getGameId()).isPresent());
        assertEquals("The Legend of Zelda", savedGame.getGameTitle());
        assertNotNull(savedGame.getGameId());
    }

    @Test
    void testFindByGameTitleContainingIgnoreCase_Success() {
        // Arrange
        Game game1 = Game.builder()
                .gameTitle("Super Mario Odyssey")
                .platform("Nintendo Switch")
                .productCode("MARIO01")
                .build();
        Game game2 = Game.builder()
                .gameTitle("Mario Kart 8 Deluxe")
                .platform("Nintendo Switch")
                .productCode("KART01")
                .build();
        Game game3 = Game.builder()
                .gameTitle("Zelda: Breath of the Wild")
                .platform("Nintendo Switch")
                .productCode("ZELDA02")
                .build();

        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();
        entityManager.clear();

        // Act - Search for games containing "mario" (case-insensitive)
        List<Game> marioGames = gameRepository.findByGameTitleContainingIgnoreCase("mario");

        // Assert
        assertNotNull(marioGames);
        assertEquals(2, marioGames.size());
        assertTrue(marioGames.stream().anyMatch(g -> g.getGameTitle().equals("Super Mario Odyssey")));
        assertTrue(marioGames.stream().anyMatch(g -> g.getGameTitle().equals("Mario Kart 8 Deluxe")));
        assertFalse(marioGames.stream().anyMatch(g -> g.getGameTitle().equals("Zelda: Breath of the Wild")));
    }

    @Test
    void testFindByPlatform_Success() {
        // Arrange
        Game switchGame = Game.builder()
                .gameTitle("Switch Game")
                .platform("Nintendo Switch")
                .productCode("SWITCH01")
                .build();
        Game xboxGame = Game.builder()
                .gameTitle("Xbox Game")
                .platform("Xbox")
                .productCode("XBOX01")
                .build();

        entityManager.persist(switchGame);
        entityManager.persist(xboxGame);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Game> switchGames = gameRepository.findByPlatform("Nintendo Switch");

        // Assert
        assertNotNull(switchGames);
        assertEquals(1, switchGames.size());
        assertEquals("Switch Game", switchGames.get(0).getGameTitle());
    }

    @Test
    void testFindByProductCode_Success() {
        // Arrange
        Game game = Game.builder()
                .gameTitle("Test Game")
                .platform("Nintendo Switch")
                .productCode("TEST123")
                .build();

        entityManager.persist(game);
        entityManager.flush();
        entityManager.clear();

        // Act
        var foundGame = gameRepository.findByProductCode("TEST123");

        // Assert
        assertTrue(foundGame.isPresent());
        assertEquals("Test Game", foundGame.get().getGameTitle());
        assertEquals("TEST123", foundGame.get().getProductCode());
    }

    @Test
    void testDeleteGame_Success() {
        // Arrange
        Game game = Game.builder()
                .gameTitle("To Delete")
                .platform("Nintendo Switch")
                .productCode("DELETE01")
                .build();

        Game savedGame = entityManager.persist(game);
        entityManager.flush();
        Integer gameId = savedGame.getGameId();

        // Act
        gameRepository.delete(savedGame);
        entityManager.flush();

        // Assert
        assertFalse(gameRepository.findById(gameId).isPresent());
    }

    @Test
    void testCountGames_Success() {
        // Arrange
        Game game1 = Game.builder()
                .gameTitle("Game 1")
                .platform("Nintendo Switch")
                .productCode("GAME01")
                .build();
        Game game2 = Game.builder()
                .gameTitle("Game 2")
                .platform("Nintendo Switch")
                .productCode("GAME02")
                .build();
        Game game3 = Game.builder()
                .gameTitle("Game 3")
                .platform("Nintendo Switch")
                .productCode("GAME03")
                .build();

        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.persist(game3);
        entityManager.flush();

        // Act
        long count = gameRepository.count();

        // Assert
        assertEquals(3, count);
    }
}
