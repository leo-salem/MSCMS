package com.example.storeservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every N seconds (default: every 10s) scans for:
 *  - SCHEDULED auctions whose start time has arrived → activate
 *  - ACTIVE auctions whose end time has passed → finalize (capture winner, mark ENDED)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionService auctionService;

    @Scheduled(cron = "${mscms.scheduling.auction-finalizer-cron:*/10 * * * * *}")
    public void tick() {
        try {
            auctionService.activateScheduledAuctions();
        } catch (Exception e) {
            log.error("activateScheduledAuctions failed", e);
        }
        try {
            auctionService.finalizeEndedAuctions();
        } catch (Exception e) {
            log.error("finalizeEndedAuctions failed", e);
        }
    }
}
