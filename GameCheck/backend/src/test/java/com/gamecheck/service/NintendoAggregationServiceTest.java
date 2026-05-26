package com.gamecheck.service;

import com.gamecheck.model.Game;
import com.gamecheck.model.Source;
import com.gamecheck.repository.GameRepository;
import com.gamecheck.repository.PriceRepository;
import com.gamecheck.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NintendoAggregationServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private PriceRepository priceRepository;

    @Mock
    private SourceRepository sourceRepository;

    @InjectMocks
    private NintendoAggregationService nintendoAggregationService;

    private Source nintendoSource;

    @BeforeEach
    void setUp() {
        nintendoSource = new Source();
        nintendoSource.setSourceId(1);
        nintendoSource.setSourceName("Nintendo eShop");
        nintendoSource.setSourceType("Digital");
        nintendoSource.setSourceUrl("https://www.nintendo.com");
    }

    @Test
    void testAggregateForTitle_SourceCreation() {
        // Arrange
        List<Game> existingGames = new ArrayList<>();
        when(gameRepository.findAll()).thenReturn(existingGames);
        when(sourceRepository.findBySourceName("Nintendo eShop")).thenReturn(Optional.empty());
        when(sourceRepository.save(any(Source.class))).thenReturn(nintendoSource);

        // Act
        var response = nintendoAggregationService.aggregateForTitle("Test Game");

        // Assert
        assertNotNull(response);
        verify(sourceRepository, times(1)).save(any(Source.class));
    }

    @Test
    void testAggregateForTitle_ExistingSource() {
        // Arrange
        List<Game> existingGames = new ArrayList<>();
        when(gameRepository.findAll()).thenReturn(existingGames);
        when(sourceRepository.findBySourceName("Nintendo eShop")).thenReturn(Optional.of(nintendoSource));

        // Act
        var response = nintendoAggregationService.aggregateForTitle("Test Game");

        // Assert
        assertNotNull(response);
        verify(sourceRepository, never()).save(any(Source.class));
    }
}
