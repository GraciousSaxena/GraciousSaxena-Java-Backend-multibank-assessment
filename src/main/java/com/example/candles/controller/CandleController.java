package com.example.candles.controller;

import com.example.candles.model.BidAskEvent;
import com.example.candles.model.Candle;
import com.example.candles.model.Interval;
import com.example.candles.service.CandleAggregationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping
@Validated
public class CandleController {
    private final CandleAggregationService aggregationService;

    public CandleController(CandleAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @GetMapping("/history")
    public HistoryResponse history(@RequestParam @NotBlank String symbol,
                                   @RequestParam String interval,
                                   @RequestParam long from,
                                   @RequestParam long to) {
        Interval parsedInterval = Interval.fromCode(interval);
        List<Candle> candles = aggregationService.getHistory(symbol, parsedInterval, from, to);
        if (candles.isEmpty()) {
            return new HistoryResponse("no_data", List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        List<Long> t = new ArrayList<>(candles.size());
        List<Double> o = new ArrayList<>(candles.size());
        List<Double> h = new ArrayList<>(candles.size());
        List<Double> l = new ArrayList<>(candles.size());
        List<Double> c = new ArrayList<>(candles.size());
        List<Long> v = new ArrayList<>(candles.size());

        for (Candle candle : candles) {
            t.add(candle.time());
            o.add(candle.open());
            h.add(candle.high());
            l.add(candle.low());
            c.add(candle.close());
            v.add(candle.volume());
        }

        return new HistoryResponse("ok", t, o, h, l, c, v);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingest(@Valid @RequestBody IngestRequest request) {
        aggregationService.process(new BidAskEvent(
                request.symbol(),
                request.bid(),
                request.ask(),
                request.timestamp()
        ));
    }
}
