package com.gamecheck.dto;

import java.math.BigDecimal;
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
public class GameSummaryDto {

    private Integer gameId;
    private String title;
    private String platform;
    /** Lowest tracked price in PHP, or null if no price rows exist for this game. */
    private BigDecimal bestPricePhp;
    private String coverImageUrl;
}
