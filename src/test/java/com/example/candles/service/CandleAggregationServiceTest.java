package com.example.candles.service;

import com.example.candles.model.BidAskEvent;
import com.example.candles.model.Candle;
import com.example.candles.model.Interval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CandleAggregationServiceTest {

    private CandleAggregationService service;

    @BeforeEach
    void setUp() {
        service = new CandleAggregationService();
    }

    @Test
    void shouldAggregateOneSecondCandleCorrectly() {
        service.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 100)); // 101.0
        service.process(new BidAskEvent("BTC-USD", 103.0, 105.0, 100)); // 104.0
        service.process(new BidAskEvent("BTC-USD", 99.0, 101.0, 100));  // 100.0

        List<Candle> candles = service.getHistory("BTC-USD", Interval.ONE_SECOND, 100, 100);

        assertThat(candles).hasSize(1);
        Candle candle = candles.get(0);
        assertThat(candle.time()).isEqualTo(100);
        assertThat(candle.open()).isEqualTo(101.0);
        assertThat(candle.high()).isEqualTo(104.0);
        assertThat(candle.low()).isEqualTo(100.0);
        assertThat(candle.close()).isEqualTo(100.0);
        assertThat(candle.volume()).isEqualTo(3);
    }

    @Test
    void shouldAlignEventsIntoSameOneMinuteBucket() {
        service.process(new BidAskEvent("ETH-USD", 200.0, 202.0, 121));
        service.process(new BidAskEvent("ETH-USD", 210.0, 212.0, 179));

        List<Candle> candles = service.getHistory("ETH-USD", Interval.ONE_MINUTE, 120, 179);

        assertThat(candles).hasSize(1);
        assertThat(candles.get(0).time()).isEqualTo(120);
        assertThat(candles.get(0).volume()).isEqualTo(2);
        assertThat(candles.get(0).open()).isEqualTo(201.0);
        assertThat(candles.get(0).close()).isEqualTo(211.0);
    }

    @Test
    void shouldSeparateSymbols() {
        service.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 100));
        service.process(new BidAskEvent("ETH-USD", 200.0, 202.0, 100));

        assertThat(service.getHistory("BTC-USD", Interval.ONE_SECOND, 100, 100)).hasSize(1);
        assertThat(service.getHistory("ETH-USD", Interval.ONE_SECOND, 100, 100)).hasSize(1);
        assertThat(service.getHistory("SOL-USD", Interval.ONE_SECOND, 100, 100)).isEmpty();
    }
}
