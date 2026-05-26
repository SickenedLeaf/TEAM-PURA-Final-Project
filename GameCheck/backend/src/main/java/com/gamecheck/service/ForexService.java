package com.gamecheck.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fetches USD-based FX tables from <a href="https://www.exchangerate-api.com/">ExchangeRate-API</a> (v6) and keeps
 * them in memory for 24 hours. Used by the aggregation layer so non-PHP amounts can be converted before persisting.
 */
@Service
@Slf4j
public class ForexService {

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    private final RestClient restClient;
    private final String apiKey;
    private final Clock clock = Clock.systemUTC();

    private final Object cacheLock = new Object();
    private volatile ConversionCache cache;

    public ForexService(
            @Value("${app.forex.api-key:}") String apiKey,
            @Value("${app.forex.base-url:https://v6.exchangerate-api.com}") String baseUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient =
                RestClient.builder()
                        .baseUrl(normalized)
                        .requestFactory(requestFactory)
                        .build();
    }

    /**
     * Returns how many Philippine pesos one US dollar buys, using the same 24-hour cached payload as {@link
     * #convertToPhp(BigDecimal, String)}.
     */
    public BigDecimal getUsdToPhpRate() {
        Map<String, BigDecimal> rates = loadRates();
        BigDecimal php = rates.get("PHP");
        if (php == null || php.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Forex response missing a positive PHP conversion rate");
        }
        return php;
    }

    /**
     * Converts an amount to PHP for storage on {@code prices.price_php}. PHP passes through (scaled to 2 dp). Any
     * other ISO code uses the cached USD table: rates are "foreign units per 1 USD" as returned by the API.
     */
    public BigDecimal convertToPhp(BigDecimal originalAmount, String currencyCode) {
        Objects.requireNonNull(originalAmount, "originalAmount");
        String code = normalizeCurrency(currencyCode);
        if ("PHP".equals(code)) {
            return originalAmount.setScale(2, RoundingMode.HALF_UP);
        }
        Map<String, BigDecimal> rates = loadRates();
        BigDecimal phpPerUsd = rates.get("PHP");
        if (phpPerUsd == null || phpPerUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Forex response missing a positive PHP conversion rate");
        }
        BigDecimal foreignPerUsd = rates.get(code);
        if (foreignPerUsd == null || foreignPerUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unsupported or missing FX rate for currency: " + code);
        }
        BigDecimal usdAmount = originalAmount.divide(foreignPerUsd, MC);
        return usdAmount.multiply(phpPerUsd).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> loadRates() {
        Instant now = clock.instant();
        ConversionCache local = cache;
        if (local != null && local.isFresh(now)) {
            return local.rates();
        }
        synchronized (cacheLock) {
            local = cache;
            if (local != null && local.isFresh(now)) {
                return local.rates();
            }
            Map<String, BigDecimal> fetched = fetchLatestUsdTable();
            cache = new ConversionCache(now, Map.copyOf(fetched));
            return cache.rates();
        }
    }

    private Map<String, BigDecimal> fetchLatestUsdTable() {
        if (apiKey.isEmpty()) {
            throw new IllegalStateException(
                    "Forex API key is not configured; set environment variable APP_FOREX_API_KEY");
        }
        try {
            JsonNode root =
                    restClient
                            .get()
                            .uri("/v6/{apiKey}/latest/USD", apiKey)
                            .retrieve()
                            .body(JsonNode.class);
            if (root == null) {
                throw new IllegalStateException("Forex API returned an empty body");
            }
            if (!"success".equalsIgnoreCase(text(root, "result"))) {
                String err = text(root, "error-type");
                if (err == null || err.isBlank()) {
                    err = root.toString();
                }
                throw new IllegalStateException("Forex API error: " + err);
            }
            JsonNode conv = root.get("conversion_rates");
            if (conv == null || !conv.isObject()) {
                throw new IllegalStateException("Forex API response missing conversion_rates");
            }
            Map<String, BigDecimal> out = new HashMap<>();
            Iterator<String> it = conv.fieldNames();
            while (it.hasNext()) {
                String ccy = it.next();
                JsonNode n = conv.get(ccy);
                if (n != null && n.isNumber()) {
                    out.put(ccy.toUpperCase(Locale.ROOT), n.decimalValue());
                }
            }
            if (out.isEmpty()) {
                throw new IllegalStateException("Forex API returned no conversion rates");
            }
            log.info("Refreshed forex cache from ExchangeRate-API ({} currencies)", out.size());
            return out;
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to call Forex API: " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static String normalizeCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalArgumentException("currencyCode is required");
        }
        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }

    private record ConversionCache(Instant fetchedAt, Map<String, BigDecimal> rates) {
        boolean isFresh(Instant now) {
            return !now.isAfter(fetchedAt.plus(CACHE_TTL));
        }
    }
}
