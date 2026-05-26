package com.gamecheck.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDto {

    private Integer priceId;
    private String sourceName;
    private String sourceType;
    private BigDecimal pricePhp;
    private String listingUrl;
    private LocalDateTime lastUpdated;
    /** True when this row matches the lowest {@link #pricePhp} in the returned list (ties all marked true). */
    private boolean cheapest;
}
