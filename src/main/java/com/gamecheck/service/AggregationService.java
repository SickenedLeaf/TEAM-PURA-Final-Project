package com.gamecheck.service;

import com.gamecheck.model.Game;
import com.gamecheck.model.Price;
import com.gamecheck.model.Source;
import com.gamecheck.repository.GameRepository;
import com.gamecheck.repository.PriceRepository;
import com.gamecheck.repository.SourceRepository;
import com.gamecheck.scraper.AggregationPriceConverter;
import com.gamecheck.scraper.PriceRecord;
import com.gamecheck.scraper.SourceAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs all registered source adapters and refreshes stored prices. Invokes wishlist price-drop checks after each
 * full run (see Milestone 6 for adapter implementations).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AggregationService {

    private final List<SourceAdapter> sourceAdapters;
    private final WishlistPriceAlertService wishlistPriceAlertService;
    private final SourceRepository sourceRepository;
    private final GameRepository gameRepository;
    private final PriceRepository priceRepository;
    private final AggregationPriceConverter priceConverter;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void runFullUpdate() {
        log.info("Starting full price aggregation ({} adapters)", sourceAdapters.size());

        for (SourceAdapter adapter : sourceAdapters) {
            List<PriceRecord> priceRecords;
            try {
                priceRecords = adapter.fetchPrices();
            } catch (Exception e) {
                log.error("Adapter {} failed: {}", adapter.getSourceName(), e.getMessage(), e);
                continue;
            }

            for (PriceRecord priceRecord : priceRecords) {
                try {
                    persistPriceRecord(priceRecord);
                } catch (Exception e) {
                    log.error(
                        "Failed to persist price record [source={}, code={}, title={}]: {}",
                        priceRecord.getSourceName(),
                        priceRecord.getProductCode(),
                        priceRecord.getGameTitle(),
                        e.getMessage(),
                        e);
                }
            }
        }

        wishlistPriceAlertService.checkAndLogPriceDropAlerts();
        log.info("Full price aggregation finished");
    }

    private void persistPriceRecord(PriceRecord priceRecord) {
        if (priceRecord == null) {
            throw new IllegalArgumentException("PriceRecord is null");
        }

        if (priceRecord.getProductCode() == null || priceRecord.getProductCode().isBlank()) {
            throw new IllegalArgumentException("Missing productCode for price record");
        }

        if (priceRecord.getSourceName() == null || priceRecord.getSourceName().isBlank()) {
            throw new IllegalArgumentException("Missing sourceName for price record");
        }

        Source source = sourceRepository.findBySourceName(priceRecord.getSourceName())
            .orElseThrow(() -> new IllegalStateException("Source not found: " + priceRecord.getSourceName()));

        Game game = gameRepository.findByProductCode(priceRecord.getProductCode())
            .orElseGet(() -> createGameFromPriceRecord(priceRecord));

        BigDecimal pricePhp = priceConverter.toStoredPricePhp(priceRecord.getPriceOriginal(), priceRecord.getCurrencyCode())
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal priceOriginal = priceRecord.getPriceOriginal().setScale(2, RoundingMode.HALF_UP);

        Price price = priceRepository.findByGameAndSource(game, source)
            .orElseGet(() -> Price.builder().game(game).source(source).build());

        price.setPricePhp(pricePhp);
        price.setPriceOriginal(priceOriginal);
        price.setCurrencyCode(priceRecord.getCurrencyCode());
        price.setListingUrl(priceRecord.getListingUrl());
        price.setLastUpdated(LocalDateTime.now());

        priceRepository.save(price);
    }

    private Game createGameFromPriceRecord(PriceRecord priceRecord) {
        Game game = Game.builder()
            .productCode(priceRecord.getProductCode())
            .gameTitle(priceRecord.getGameTitle() == null || priceRecord.getGameTitle().isBlank()
                ? "Unknown Title"
                : priceRecord.getGameTitle())
            .platform(priceRecord.getPlatform() == null || priceRecord.getPlatform().isBlank()
                ? "Nintendo Switch"
                : priceRecord.getPlatform())
            .build();

        Game savedGame = gameRepository.save(game);
        updateSearchVector(savedGame);
        return savedGame;
    }

    private void updateSearchVector(Game game) {
        if (game.getGameId() == null) {
            return;
        }

        entityManager.createNativeQuery(
                "UPDATE games SET search_vector = to_tsvector('english', coalesce(game_title, '') || ' ' || coalesce(platform, '')) WHERE game_id = :gameId")
            .setParameter("gameId", game.getGameId())
            .executeUpdate();
    }
}
