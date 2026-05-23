package com.gamecheck.service;

import com.gamecheck.scraper.SourceAdapter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public void runFullUpdate() {
        log.info("Starting full price aggregation ({} adapters)", sourceAdapters.size());
        for (SourceAdapter adapter : sourceAdapters) {
            try {
                adapter.fetchPrices();
            } catch (Exception e) {
                log.error("Adapter {} failed: {}", adapter.getSourceName(), e.getMessage(), e);
            }
        }
        wishlistPriceAlertService.checkAndLogPriceDropAlerts();
        log.info("Full price aggregation finished");
    }
}
