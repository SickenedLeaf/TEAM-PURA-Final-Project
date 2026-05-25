package com.gamecheck.service;

import com.gamecheck.model.Wishlist;
import com.gamecheck.repository.PriceRepository;
import com.gamecheck.repository.WishlistRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * After price aggregation, checks wishlist entries with an alert threshold and logs when the game's lowest
 * tracked price is at or below that threshold (email notifications can be added later).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistPriceAlertService {

    private final WishlistRepository wishlistRepository;
    private final PriceRepository priceRepository;

    @Transactional(readOnly = true)
    public void checkAndLogPriceDropAlerts() {
        List<Wishlist> alerts = wishlistRepository.findAllWithPriceAlertThreshold();
        for (Wishlist w : alerts) {
            Integer gameId = w.getGame().getGameId();
            BigDecimal threshold = w.getPriceAlertThreshold();
            BigDecimal best =
                    priceRepository.findMinPricePhpByGame_GameId(gameId).orElse(null);
            if (best == null || threshold == null) {
                continue;
            }
            if (best.compareTo(threshold) <= 0) {
                log.warn(
                        "Price drop alert: userId={} gameId={} title=\"{}\" thresholdPhp={} bestPricePhp={}",
                        w.getUser().getUserId(),
                        gameId,
                        w.getGame().getGameTitle(),
                        threshold,
                        best);
            }
        }
    }
}
