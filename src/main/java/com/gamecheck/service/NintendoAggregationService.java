package com.gamecheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamecheck.dto.NintendoGameDto;
import com.gamecheck.model.Game;
import com.gamecheck.model.Price;
import com.gamecheck.model.Source;
import com.gamecheck.repository.GameRepository;
import com.gamecheck.repository.PriceRepository;
import com.gamecheck.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NintendoAggregationService {

    private static final Logger logger = LoggerFactory.getLogger(NintendoAggregationService.class);
    private static final double FALLBACK_USD_TO_PHP_RATE = 56.0;
    private static final String NINTENDO_US_ALGOLIA_APP_ID = "U3B6MG4I2R";
    private static final String NINTENDO_US_ALGOLIA_API_KEY = "9fa3d63fbd3d277a9ec5536159da3248";
    private static final String NINTENDO_US_ALGOLIA_INDEX = "ncom_game_us_en_title";
    private static final String FOREX_API_URL = "https://api.exchangerate-api.com/v4/latest/USD";
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour

    private final GameRepository gameRepository;
    private final PriceRepository priceRepository;
    private final SourceRepository sourceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Cache for exchange rate
    private Double cachedUsdToPhpRate = null;
    private long rateCacheTimestamp = 0;

    public NintendoAggregationService(GameRepository gameRepository, PriceRepository priceRepository, SourceRepository sourceRepository) {
        this.gameRepository = gameRepository;
        this.priceRepository = priceRepository;
        this.sourceRepository = sourceRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void aggregateEShopPrices() {
        // 1. Fetch all existing games from our DB
        List<Game> existingGames = gameRepository.findAll();

        // 2. Create a fast-lookup Map using Normalized Titles as the key
        Map<String, Game> gameMap = existingGames.stream()
            .collect(Collectors.toMap(
                game -> normalizeTitle(game.getGameTitle()),
                game -> game,
                (existing, replacement) -> existing // Ignore duplicates
            ));

        // 3. Fetch data from the Nintendo API
        List<NintendoGameDto> nintendoData = fetchFromNintendoApi();

        // 4. Get or create Nintendo eShop source
        Source nintendoSource = getOrCreateNintendoEShopSource();

        // 5. Match and Aggregate
        for (NintendoGameDto nintendoGame : nintendoData) {
            String normalizedEShopTitle = normalizeTitle(nintendoGame.getTitle());

            if (gameMap.containsKey(normalizedEShopTitle)) {
                Game matchedGame = gameMap.get(normalizedEShopTitle);

                // Check if price already exists for this game and source
                Optional<Price> existingPrice = priceRepository.findByGameAndSource(matchedGame, nintendoSource);
                
                if (existingPrice.isEmpty()) {
                    Price digitalListing = new Price();
                    digitalListing.setGame(matchedGame);
                    digitalListing.setSource(nintendoSource);
                    digitalListing.setPricePhp(convertUsdToPhp(nintendoGame.getPrice()));
                    digitalListing.setPriceOriginal(nintendoGame.getPrice()); // Store original USD price
                    digitalListing.setCurrencyCode("USD");
                    digitalListing.setListingUrl(nintendoGame.getUrl());
                    digitalListing.setLastUpdated(LocalDateTime.now());
                    
                    priceRepository.save(digitalListing);
                }
            } else {
                // Game doesn't exist in our database - create it as a digital-exclusive game
                Game newGame = new Game();
                newGame.setGameTitle(nintendoGame.getTitle());
                newGame.setPlatform("Nintendo Switch");
                newGame.setCoverImageUrl(nintendoGame.getCoverImageUrl());
                
                Game savedGame = gameRepository.save(newGame);
                
                // Create price listing for the new game
                Price digitalListing = new Price();
                digitalListing.setGame(savedGame);
                digitalListing.setSource(nintendoSource);
                digitalListing.setPricePhp(convertUsdToPhp(nintendoGame.getPrice()));
                digitalListing.setPriceOriginal(nintendoGame.getPrice());
                digitalListing.setCurrencyCode("USD");
                digitalListing.setListingUrl(nintendoGame.getUrl());
                digitalListing.setLastUpdated(LocalDateTime.now());
                
                priceRepository.save(digitalListing);
                
                logger.info("Created new digital-exclusive game: {}", nintendoGame.getTitle());
            }
        }
    }

    // Normalizes titles for fuzzy matching
    private String normalizeTitle(String rawTitle) {
        if (rawTitle == null) return "";
        return rawTitle.toLowerCase()
                .replaceAll("\\(us\\)", "")
                .replaceAll("\\(eu\\)", "")
                .replaceAll("\\(asian\\)", "")
                .replaceAll("™", "")
                .replaceAll("®", "")
                .replaceAll("[^a-z0-9]", "");
    }

    // Get or create Nintendo eShop source
    private Source getOrCreateNintendoEShopSource() {
        Optional<Source> existingSource = sourceRepository.findBySourceName("Nintendo eShop");
        if (existingSource.isPresent()) {
            return existingSource.get();
        }
        
        Source newSource = new Source();
        newSource.setSourceName("Nintendo eShop");
        newSource.setSourceType("Digital");
        newSource.setSourceUrl("https://www.nintendo.com");
        return sourceRepository.save(newSource);
    }

    // Convert USD to PHP using cached or live exchange rate
    private BigDecimal convertUsdToPhp(BigDecimal usdPrice) {
        double rate = getUsdToPhpRate();
        return usdPrice.multiply(new BigDecimal(rate));
    }

    // Get USD to PHP exchange rate with caching
    private double getUsdToPhpRate() {
        long currentTime = System.currentTimeMillis();
        
        // Return cached rate if still valid (within 1 hour)
        if (cachedUsdToPhpRate != null && (currentTime - rateCacheTimestamp) < CACHE_DURATION_MS) {
            return cachedUsdToPhpRate;
        }
        
        // Fetch live rate from Forex API
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(FOREX_API_URL, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode rates = root.path("rates");
                JsonNode phpRate = rates.path("PHP");
                
                if (phpRate != null && !phpRate.isMissingNode()) {
                    double rate = phpRate.asDouble();
                    cachedUsdToPhpRate = rate;
                    rateCacheTimestamp = currentTime;
                    logger.info("Updated USD to PHP exchange rate: {}", rate);
                    return rate;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch live exchange rate, using fallback: {}", e.getMessage());
        }
        
        // Fallback to hardcoded rate if API fails
        if (cachedUsdToPhpRate == null) {
            cachedUsdToPhpRate = FALLBACK_USD_TO_PHP_RATE;
            rateCacheTimestamp = currentTime;
        }
        
        return cachedUsdToPhpRate;
    }

    // Fetch games from Nintendo US eShop Algolia API
    private List<NintendoGameDto> fetchFromNintendoApi() {
        List<NintendoGameDto> games = new ArrayList<>();
        
        try {
            // Build Algolia query
            String algoliaUrl = "https://" + NINTENDO_US_ALGOLIA_APP_ID + "-dsn.algolia.net/1/indexes/" + NINTENDO_US_ALGOLIA_INDEX + "/query";
            
            // Create request body
            String requestBody = "{\"params\":\"hitsPerPage=100&facetFilters=[\\\"platform:Nintendo Switch\\\"]\"}";
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Algolia-API-Key", NINTENDO_US_ALGOLIA_API_KEY);
            headers.set("X-Algolia-Application-Id", NINTENDO_US_ALGOLIA_APP_ID);
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // Make POST request
            ResponseEntity<String> response = restTemplate.exchange(algoliaUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode hits = root.path("hits");
                
                for (JsonNode hit : hits) {
                    String title = hit.path("title").asText();
                    if (title == null || title.isBlank()) {
                        continue;
                    }
                    
                    // Extract price (price_range_regular or price_range_low)
                    String priceStr = hit.path("price_range_regular").asText();
                    if (priceStr == null || priceStr.isBlank()) {
                        priceStr = hit.path("price_range_low").asText();
                    }
                    
                    BigDecimal price = parsePrice(priceStr);
                    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    // Extract URL
                    String url = hit.path("url").asText();
                    if (url == null || url.isBlank()) {
                        url = "https://www.nintendo.com/store/games/";
                    }
                    
                    // Extract cover image URL
                    String coverImageUrl = hit.path("boxart").asText();
                    if (coverImageUrl == null || coverImageUrl.isBlank()) {
                        coverImageUrl = hit.path("image_url").asText();
                    }
                    
                    NintendoGameDto dto = NintendoGameDto.builder()
                        .title(title)
                        .price(price)
                        .url(url)
                        .coverImageUrl(coverImageUrl)
                        .build();
                    
                    games.add(dto);
                }
                
                logger.info("Successfully fetched {} games from Nintendo eShop API", games.size());
            } else {
                logger.error("Nintendo eShop API returned non-2xx status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error fetching games from Nintendo eShop API: {}", e.getMessage(), e);
        }
        
        return games;
    }
    
    // Parse price string to BigDecimal
    private BigDecimal parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) {
            return null;
        }
        
        try {
            // Remove currency symbols and commas
            String normalized = priceStr.replaceAll("[^0-9.]", "");
            if (normalized.isBlank()) {
                return null;
            }
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse price: {}", priceStr);
            return null;
        }
    }
}
