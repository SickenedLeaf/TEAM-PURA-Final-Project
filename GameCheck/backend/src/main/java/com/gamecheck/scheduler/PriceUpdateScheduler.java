package com.gamecheck.scheduler;

import com.gamecheck.service.AggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateScheduler {

    private final AggregationService aggregationService;

    /**
     * Daily full aggregation run at 2 AM local time.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyAggregation() {
        log.info("Scheduled aggregation starting at 2 AM");
        aggregationService.runFullUpdate();
    }
}
