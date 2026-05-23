package com.gamecheck.scraper;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Standardized scraped price row passed into the aggregation layer. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRecord {

    private String gameTitle;
    private String platform;
    private BigDecimal priceOriginal;
    private String currencyCode;
    private String listingUrl;
    private String sourceName;
}
