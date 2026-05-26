package com.gamecheck.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Adapter for Nintendo eShop API (Europe region).
 * Fetches Nintendo Switch game prices from the Nintendo Europe search API.
 * Prices are in USD and require currency conversion to PHP via AggregationPriceConverter.
 *
 * NOTE: After fixing currency code and listing URLs, existing eShop price records in the DB
 * have wrong values and need to be cleared and re-aggregated.
 *
 * DISABLED: Replaced by NintendoAggregationService which uses the US Algolia API.
 */
// @Component
public class NintendoEshopAdapter implements SourceAdapter {

    private static final String ESHOP_API_URL = "https://searching.nintendo-europe.com/en/select?q=*&fq=type:GAME+AND+system_type:nintendoswitch*+AND+price_sorting_f:[1+TO+99999]+AND+date_from:[*+TO+NOW]&rows=50&start=0";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NintendoEshopAdapter() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getSourceName() {
        return "Nintendo eShop";
    }

    @Override
    public List<PriceRecord> fetchPrices() {
        List<PriceRecord> records = new ArrayList<>();

        try {
            String jsonResponse = restTemplate.getForObject(ESHOP_API_URL, String.class);
            if (jsonResponse == null || jsonResponse.isBlank()) {
                System.err.println("[Nintendo eShop] Empty response from API");
                return records;
            }

            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode games = root.path("response").path("docs");


            for (JsonNode game : games) {
                // Extract game title (prefer title_extras_txt, fallback to title)
                String title = game.path("title_extras_txt").asText();
                if (title.isBlank()) {
                    title = game.path("title").asText();
                }
                if (title.isBlank()) {
                    continue;
                }

                // Extract price (price_sorting_f)
                String priceStr = game.path("price_sorting_f").asText();
                BigDecimal priceOriginal = parsePrice(priceStr);
                if (priceOriginal == null || priceOriginal.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Skip placeholder prices (999999)
                if (priceOriginal.compareTo(new BigDecimal("999999")) >= 0) {
                    continue;
                }

                // Extract image URL (image_url_sq_s)
                String imageUrl = game.path("image_url_sq_s").asText();
                if (imageUrl.isBlank()) {
                    imageUrl = game.path("image_url").asText();
                }

                // Extract title_master_s as product code (clean game title without variants)
                String titleMaster = game.path("title_master_s").asText();
                if (titleMaster.isBlank()) {
                    System.err.println("[Nintendo eShop] Missing title_master_s for game: " + title);
                    continue;
                }
                String productTitleClean = generateUniqueCode(titleMaster);
                String productCode = hashString(productTitleClean);

                // Build listing URL using url field if available, otherwise fallback to store games page
                String urlField = game.path("url").asText();
                String url;
                if (!urlField.isBlank()) {
                    url = "https://www.nintendo.com" + urlField;
                } else {
                    url = "https://www.nintendo.com/store/games/";
                }

                PriceRecord record = PriceRecord.builder()
                    .productCode(productCode)
                    .gameTitle(title)
                    .platform("Nintendo Switch")
                    .priceOriginal(priceOriginal)
                    .currencyCode("USD")
                    .listingUrl(url)
                    .sourceName(getSourceName())
                    .imageUrl(imageUrl)
                    .build();

                records.add(record);
            }

            System.out.println("[Nintendo eShop] Fetched " + records.size() + " games");

        } catch (Exception e) {
            System.err.println("[Nintendo eShop] Error fetching prices: " + e.getMessage());
            e.printStackTrace();
        }

        return records;
    }

    private BigDecimal parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) {
            return null;
        }

        // Remove currency symbols and commas
        String normalized = priceStr.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected String generateUniqueCode(String rawTitle) {
        String cleanText = rawTitle.toLowerCase();

        // 1. Strip platform and hardware branding
        cleanText = cleanText.replace("nintendo switch 2", "")
                             .replace("nintendo switch", "")
                             .replace("nintendo", "")
                             .replace("switch 2", "")
                             .replace("switch", "")
                             .replace("ns2", "")
                             .replace("ns", "");

        // 2. Strip storefront packaging and market regional tags
        cleanText = cleanText.replace("mde", "")
                             .replace("asi", "")
                             .replace("us/eng/fr", "")
                             .replace("us/eng", "")
                             .replace("eng", "")
                             .replace("standard edition", "")
                             .replace("edition", "");

        // 3. Flatten remaining characters using alpha-numeric regex matching
        cleanText = cleanText.replaceAll("[^a-zA-Z0-9]", "").trim();

        // 4. Pass down to the mathematical hashing layer
        return hashString(cleanText);
    }

    /**
     * Converts a baseline normalized text string into a deterministic 
     * 8-character SHA-256 hex signature.
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            // Extract the first 4 bytes to establish a short, compact 8-character ID
            for (int i = 0; i < 4; i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
            
        } catch (NoSuchAlgorithmException e) {
            // Unconditional fallback to basic absolute hash code if system crypto fails
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }

    private String truncateOrHash(String input) {
        // If input fits in VARCHAR(20), use it directly
        if (input.length() <= 20) {
            return input.toUpperCase();
        }
        // Otherwise, truncate to 20 characters
        return input.substring(0, 20).toUpperCase();
    }
}
