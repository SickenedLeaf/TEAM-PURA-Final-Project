package com.gamecheck.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddWishlistRequest {

    /** Optional alert price in PHP; omit or null for no price-drop alert. */
    private BigDecimal priceAlertThreshold;
}
