package com.gamecheck.service;

import com.gamecheck.dto.GameDetailDto;
import com.gamecheck.dto.GameSummaryDto;
import com.gamecheck.dto.PriceDto;
import com.gamecheck.exception.ResourceNotFoundException;
import com.gamecheck.model.Game;
import com.gamecheck.model.Price;
import com.gamecheck.repository.GameRepository;
import com.gamecheck.repository.PriceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;
    private final PriceRepository priceRepository;

    /**
     * Full-text search on {@code game_title} and {@code platform} (PostgreSQL {@code tsvector}). Returns an empty list
     * when {@code query} is blank. Optional {@code platform} narrows results. Sorted by {@code bestPricePhp} (default
     * {@code price_asc}); games with no prices ({@code null} best price) are listed last.
     */
    public List<GameSummaryDto> searchGames(String query, String platform, String sort) {
        String q = query == null ? "" : query.trim();
        String p = platform == null ? "" : platform.trim();

        if (q.isEmpty()) {
            return List.of();
        }

        List<Game> games =
                p.isEmpty()
                        ? gameRepository.searchByFullText(q)
                        : gameRepository.searchByFullTextAndPlatform(q, p);

        List<GameSummaryDto> out = new ArrayList<>(games.size());
        for (Game g : games) {
            Optional<BigDecimal> best = priceRepository.findMinPricePhpByGame_GameId(g.getGameId());
            out.add(
                    GameSummaryDto.builder()
                            .gameId(g.getGameId())
                            .title(g.getGameTitle())
                            .platform(g.getPlatform())
                            .bestPricePhp(best.orElse(null))
                            .build());
        }
        out.sort(priceSortComparator(parseSearchSort(sort)));
        return out;
    }

    private static String parseSearchSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "price_asc";
        }
        String normalized = sort.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("price_asc") && !normalized.equals("price_desc")) {
            throw new IllegalArgumentException("sort must be 'price_asc' or 'price_desc'");
        }
        return normalized;
    }

    private static Comparator<GameSummaryDto> priceSortComparator(String sort) {
        Comparator<BigDecimal> priceOrder =
                "price_desc".equals(sort)
                        ? Comparator.nullsLast(Comparator.reverseOrder())
                        : Comparator.nullsLast(Comparator.naturalOrder());
        return Comparator.comparing(GameSummaryDto::getBestPricePhp, priceOrder);
    }

    /** Returns one game by id, or throws {@link ResourceNotFoundException} if missing. */
    public GameDetailDto getGameById(Integer id) {
        Game g =
                gameRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Game not found: " + id));
        return toDetailDto(g);
    }

    /**
     * Returns price rows for a game, sorted by PHP amount. Optional {@code sourceType} filters to physical or
     * digital. {@code sortDirection} is {@code asc} (default) or {@code desc}. Each row includes {@code lastUpdated}
     * and a {@code cheapest} flag (all rows tied for lowest price are marked cheapest).
     */
    public List<PriceDto> getPricesForGame(Integer gameId, String sortDirection, String sourceType) {
        if (!gameRepository.existsById(gameId)) {
            throw new ResourceNotFoundException("Game not found: " + gameId);
        }

        String sort = sortDirection == null ? "asc" : sortDirection.trim().toLowerCase(Locale.ROOT);
        if (!sort.equals("asc") && !sort.equals("desc")) {
            throw new IllegalArgumentException("sort must be 'asc' or 'desc'");
        }

        String typeFilter = sourceType == null ? null : sourceType.trim().toLowerCase(Locale.ROOT);
        if (typeFilter != null && !typeFilter.isEmpty() && !typeFilter.equals("physical") && !typeFilter.equals("digital")) {
            throw new IllegalArgumentException("sourceType must be 'physical', 'digital', or omitted");
        }

        List<Price> rows = priceRepository.findByGameIdWithSource(gameId);
        if (typeFilter != null && !typeFilter.isEmpty()) {
            final String tf = typeFilter;
            rows = rows.stream().filter(p -> tf.equals(p.getSource().getSourceType())).toList();
        }

        Comparator<Price> byAmount =
                sort.equals("asc")
                        ? Comparator.comparing(Price::getPricePhp)
                        : Comparator.comparing(Price::getPricePhp).reversed();
        rows = new ArrayList<>(rows);
        rows.sort(byAmount.thenComparing(p -> p.getSource().getSourceName(), String.CASE_INSENSITIVE_ORDER));

        BigDecimal min =
                rows.stream().map(Price::getPricePhp).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);

        List<PriceDto> dtos = new ArrayList<>(rows.size());
        for (Price p : rows) {
            boolean cheapest = min != null && p.getPricePhp() != null && p.getPricePhp().compareTo(min) == 0;
            dtos.add(
                    PriceDto.builder()
                            .priceId(p.getPriceId())
                            .sourceName(p.getSource().getSourceName())
                            .sourceType(p.getSource().getSourceType())
                            .pricePhp(p.getPricePhp())
                            .listingUrl(p.getListingUrl())
                            .lastUpdated(p.getLastUpdated())
                            .cheapest(cheapest)
                            .build());
        }
        return dtos;
    }

    private static GameDetailDto toDetailDto(Game g) {
        return GameDetailDto.builder()
                .gameId(g.getGameId())
                .title(g.getGameTitle())
                .platform(g.getPlatform())
                .metacriticScore(g.getMetacriticScore())
                .trailerUrl(g.getTrailerUrl())
                .coverImageUrl(g.getCoverImageUrl())
                .build();
    }
}
