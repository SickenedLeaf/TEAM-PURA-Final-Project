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
public class WishlistItemDto {

    private Integer wishlistId;
    private Integer gameId;
    private String title;
    private String platform;
    private BigDecimal priceAlertThreshold;
    private LocalDateTime addedAt;
    /** Current lowest tracked price in PHP, or null if no prices exist for this game. */
    private BigDecimal bestPricePhp;
}
