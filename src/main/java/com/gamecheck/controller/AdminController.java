package com.gamecheck.controller;

import com.gamecheck.service.AggregationService;
import com.gamecheck.service.NintendoAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary administrative endpoints for testing and manual operations.
 * NOTE: secure or remove this endpoint before production deployment.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AggregationService aggregationService;
    private final NintendoAggregationService nintendoAggregationService;

    @PostMapping("/trigger-aggregation")
    public ResponseEntity<String> triggerAggregation() {
        aggregationService.runFullUpdate();
        nintendoAggregationService.aggregateEShopPrices();
        return ResponseEntity.ok("Aggregation triggered");
    }
}
