package com.gamecheck.scraper;

import com.gamecheck.service.ForexService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bridge used by the data aggregation layer (adapters + future {@code AggregationService}) to resolve {@code
 * prices.price_php} from scraped {@code price_original} and {@code currency_code}. Always call this before
 * persisting a price row when the listing currency may not be PHP.
 */
@Component
@RequiredArgsConstructor
public class AggregationPriceConverter {

    private final ForexService forexService;

    /** Converts a listing amount to PHP for storage; PHP listings are only scaled to two decimal places. */
    public BigDecimal toStoredPricePhp(BigDecimal priceOriginal, String currencyCode) {
        return forexService.convertToPhp(priceOriginal, currencyCode);
    }

    /** Exposes the cached USD→PHP rate (same 24-hour cache as conversions). */
    public BigDecimal usdToPhpRate() {
        return forexService.getUsdToPhpRate();
    }
}
