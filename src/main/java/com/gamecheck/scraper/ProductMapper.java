package com.gamecheck.scraper;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Converts a scraped {@link Product} payload into an aggregation-ready {@link PriceRecord}.
 */
@Component
public class ProductMapper {

    /**
     * Maps a scraped product payload into a standardized price record.
     * Returns {@link Optional#empty()} when the price value cannot be parsed.
     */
    public Optional<PriceRecord> map(Product product) {
        if (product == null) {
            return Optional.empty();
        }

        BigDecimal priceOriginal = parsePrice(product.getPrice());
        if (priceOriginal == null) {
            return Optional.empty();
        }

        PriceRecord record = PriceRecord.builder()
            .productCode(product.getProductCode())
            .gameTitle(product.getTitle())
            .platform(normalizePlatform(product.getPlatform()))
            .priceOriginal(priceOriginal)
            .currencyCode("PHP")
            .listingUrl(product.getUrl())
            .sourceName(product.getStoreName())
            .imageUrl(product.getBoxArtUrl())
            .build();

        return Optional.of(record);
    }

    private BigDecimal parsePrice(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            return null;
        }

        String normalized = rawPrice.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizePlatform(String platform) {
    if (platform == null || platform.isBlank()) {
        return "Nintendo Switch"; // Default fallback
    }
    
    String normalized = platform.toLowerCase().trim();
    
    // Check for Switch 2 indicators first
    if (normalized.contains("switch 2") || normalized.contains("switch2")) {
        return "Nintendo Switch 2";
    }
    
    // Fallback to standard Switch
    return "Nintendo Switch";
    }
}
