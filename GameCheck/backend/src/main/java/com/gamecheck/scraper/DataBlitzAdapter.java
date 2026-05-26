package com.gamecheck.scraper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataBlitzAdapter implements SourceAdapter {

    private final ProductMapper productMapper;
    private final int urlCap;

    public DataBlitzAdapter(ProductMapper productMapper,
                            @Value("${scraper.url-cap:10}") int urlCap) {
        this.productMapper = productMapper;
        this.urlCap = urlCap;
    }

    @Override
    public String getSourceName() {
        return "DataBlitz";
    }

    @Override
    public List<PriceRecord> fetchPrices() {
        System.out.println("[DataBlitz] Starting scrape...");
        try {
            DatablitzScraper scraper = new DatablitzScraper();
            return scraper.syncStore(urlCap).stream()
                .map(productMapper::map)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[DataBlitz] Fatal error: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}
