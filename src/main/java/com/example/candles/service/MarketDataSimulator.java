package com.example.candles.service;

import com.example.candles.model.BidAskEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class MarketDataSimulator {
    private static final Logger log = LoggerFactory.getLogger(MarketDataSimulator.class);

    private final CandleAggregationService aggregationService;
    private final boolean enabled;
    private final Map<String, Double> lastMidPrices = new LinkedHashMap<>();

    public MarketDataSimulator(CandleAggregationService aggregationService,
                               @Value("${market.simulator.enabled:true}") boolean enabled) {
        this.aggregationService = aggregationService;
        this.enabled = enabled;
        lastMidPrices.put("BTC-USD", 65000.0);
        lastMidPrices.put("ETH-USD", 3200.0);
        lastMidPrices.put("SOL-USD", 145.0);
    }

    @PostConstruct
    void onStartup() {
        if (enabled) {
            log.info("Market data simulator enabled for symbols={}", lastMidPrices.keySet());
        } else {
            log.info("Market data simulator disabled");
        }
    }

    @PreDestroy
    void onShutdown() {
        log.info("Shutting down market data simulator safely");
    }

    @Scheduled(fixedDelayString = "${market.simulator.delay-ms:1000}")
    public void publish() {
        if (!enabled) {
            return;
        }

        long now = Instant.now().getEpochSecond();
        for (Map.Entry<String, Double> entry : lastMidPrices.entrySet()) {
            double nextMid = nudge(entry.getValue());
            double spread = Math.max(nextMid * 0.0002, 0.01);
            double bid = round(nextMid - (spread / 2.0));
            double ask = round(nextMid + (spread / 2.0));
            entry.setValue(nextMid);

            BidAskEvent event = new BidAskEvent(entry.getKey(), bid, ask, now);
            aggregationService.process(event);
            log.debug("Published simulated event: {}", event);
        }
    }

    private double nudge(double current) {
        double change = ThreadLocalRandom.current().nextDouble(-0.003, 0.003);
        return round(current * (1.0 + change));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
