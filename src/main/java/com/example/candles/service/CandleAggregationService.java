package com.example.candles.service;

import com.example.candles.model.BidAskEvent;
import com.example.candles.model.Candle;
import com.example.candles.model.Interval;
import com.example.candles.storage.MutableCandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Service
public class CandleAggregationService {
    private static final Logger log = LoggerFactory.getLogger(CandleAggregationService.class);

    private final ConcurrentHashMap<String, ConcurrentHashMap<Interval, ConcurrentSkipListMap<Long, MutableCandle>>> store = new ConcurrentHashMap<>();

    public void process(BidAskEvent event) {
        validate(event);
        double price = midPrice(event.bid(), event.ask());

        for (Interval interval : Interval.values()) {
            long bucketTime = interval.align(event.timestamp());
            MutableCandle candle = store
                    .computeIfAbsent(event.symbol(), ignored -> new ConcurrentHashMap<>())
                    .computeIfAbsent(interval, ignored -> new ConcurrentSkipListMap<>())
                    .compute(bucketTime, (ignored, existing) -> {
                        if (existing == null) {
                            MutableCandle created = new MutableCandle(bucketTime, price);
                            log.debug("Created new candle for symbol={} interval={} time={} price={}",
                                    event.symbol(), interval.code(), bucketTime, price);
                            return created;
                        }
                        existing.update(price);
                        return existing;
                    });

            if (candle != null && candle.snapshot().volume() == 1L) {
                log.info("Candle opened: symbol={} interval={} time={} open={}",
                        event.symbol(), interval.code(), bucketTime, price);
            }
        }

        log.debug("Processed event: {}", event);
    }

    public List<Candle> getHistory(String symbol, Interval interval, long from, long to) {
        Map<Interval, ConcurrentSkipListMap<Long, MutableCandle>> symbolMap = store.get(symbol);
        if (symbolMap == null) {
            return List.of();
        }
        ConcurrentNavigableMap<Long, MutableCandle> candles = symbolMap.get(interval);
        if (candles == null) {
            return List.of();
        }

        List<Candle> result = new ArrayList<>();
        candles.subMap(from, true, to, true)
                .forEach((time, candle) -> result.add(candle.snapshot()));
        return result;
    }

    public void clear() {
        store.clear();
    }

    private void validate(BidAskEvent event) {
        if (event.symbol() == null || event.symbol().isBlank()) {
            throw new IllegalArgumentException("Symbol must not be blank");
        }
        if (event.timestamp() < 0) {
            throw new IllegalArgumentException("Timestamp must be non-negative");
        }
        if (event.bid() <= 0 || event.ask() <= 0) {
            throw new IllegalArgumentException("Bid and ask must be positive");
        }
        if (event.bid() > event.ask()) {
            throw new IllegalArgumentException("Bid must be less than or equal to ask");
        }
    }

    private double midPrice(double bid, double ask) {
        return (bid + ask) / 2.0;
    }
}
