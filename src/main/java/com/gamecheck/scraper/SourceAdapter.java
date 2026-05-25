package com.gamecheck.scraper;

import java.util.List;

/**
 * One implementation per retailer/data source (Milestone 6). Registered automatically as Spring beans.
 */
public interface SourceAdapter {

    String getSourceName();

    List<PriceRecord> fetchPrices();
}
