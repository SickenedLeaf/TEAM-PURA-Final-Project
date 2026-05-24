package com.gamecheck.scraper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ITechAdapter implements SourceAdapter {

    private final ProductMapper productMapper;
    private final int urlCap;

    public ITechAdapter(ProductMapper productMapper,
                        @Value("${scraper.url-cap:10}") int urlCap) {
        this.productMapper = productMapper;
        this.urlCap = urlCap;
    }

    @Override
    public String getSourceName() {
        return "iTech";
    }

    @Override
    public List<PriceRecord> fetchPrices() {
        ITechScraper scraper = new ITechScraper();
        return scraper.syncStore(urlCap).stream()
            .map(productMapper::map)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }
}
