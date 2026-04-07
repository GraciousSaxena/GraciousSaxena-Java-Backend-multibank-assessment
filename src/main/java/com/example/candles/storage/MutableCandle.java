package com.example.candles.storage;

import com.example.candles.model.Candle;

public class MutableCandle {
    private final long time;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    public MutableCandle(long time, double initialPrice) {
        this.time = time;
        this.open = initialPrice;
        this.high = initialPrice;
        this.low = initialPrice;
        this.close = initialPrice;
        this.volume = 1L;
    }

    public synchronized void update(double price) {
        this.high = Math.max(this.high, price);
        this.low = Math.min(this.low, price);
        this.close = price;
        this.volume++;
    }

    public synchronized Candle snapshot() {
        return new Candle(time, open, high, low, close, volume);
    }
}
