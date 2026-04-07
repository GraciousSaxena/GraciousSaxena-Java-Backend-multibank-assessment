# Candle Aggregation Service

A runnable Spring Boot + Maven backend that ingests bid/ask market data, aggregates it into OHLC candles for multiple symbols and intervals, and exposes historical candle data through a REST API.

## What is implemented

- Real-time simulated market data ingestion using a scheduled publisher
- Thread-safe in-memory aggregation for:
  - Symbols such as `BTC-USD`, `ETH-USD`, `SOL-USD`
  - Intervals `1s`, `5s`, `1m`, `15m`, `1h`
- REST history endpoint compatible with TradingView-style arrays:
  - `GET /history?symbol=BTC-USD&interval=1m&from=1712448000&to=1712448600`
- Manual ingestion endpoint for testing or replay:
  - `POST /events`
- Observability:
  - Spring Boot Actuator health endpoint
  - Logging for candle creation and simulator lifecycle
- Tests covering aggregation logic and REST API behavior

## Design choices and trade-offs

- **Storage**: Uses an in-memory `ConcurrentHashMap + ConcurrentSkipListMap` design, which satisfies the minimum requirement and keeps the project easy to run locally.
- **Price source for OHLC**: Uses the midpoint `(bid + ask) / 2` as candle price.
- **Volume**: Uses synthetic volume equal to the number of ticks in the candle.
- **Delayed events**: Events are aligned into the proper candle bucket using their own event timestamp, so slight delays are handled as long as the event eventually arrives.
- **Extensibility**: Timeframe logic is centralized in the `Interval` enum; new intervals can be added in one place.

## Tech stack

- Java 17
- Spring Boot 3
- Maven
- Spring Web
- Spring Actuator
- JUnit 5 / MockMvc

## How to run locally

### 1) Start the application

```bash
mvn spring-boot:run
```

Or build and run the jar:

```bash
mvn clean package
java -jar target/candle-aggregation-service-1.0.0.jar
```

The app starts on `http://localhost:8080`.

### 2) Health check

```bash
curl http://localhost:8080/actuator/health
```

### 3) Query history

Wait a few seconds after startup so the simulator can generate data, then run:

```bash
curl "http://localhost:8080/history?symbol=BTC-USD&interval=1s&from=0&to=9999999999"
```

### 4) Manually ingest an event

```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "BTC-USD",
    "bid": 65000.10,
    "ask": 65000.30,
    "timestamp": 1712448000
  }'
```

Then query:

```bash
curl "http://localhost:8080/history?symbol=BTC-USD&interval=1s&from=1712448000&to=1712448000"
```

## API contract

### GET /history

Query params:

- `symbol`: market symbol, for example `BTC-USD`
- `interval`: one of `1s`, `5s`, `1m`, `15m`, `1h`
- `from`: start UNIX timestamp in seconds
- `to`: end UNIX timestamp in seconds

Example response:

```json
{
  "s": "ok",
  "t": [1712448000],
  "o": [65000.2],
  "h": [65001.1],
  "l": [64999.8],
  "c": [65000.9],
  "v": [4]
}
```

If no candles exist in the requested range:

```json
{
  "s": "no_data",
  "t": [],
  "o": [],
  "h": [],
  "l": [],
  "c": [],
  "v": []
}
```

## Running tests

```bash
mvn test
```

## Notes on bonus-friendly extensibility

The project is structured so we can later add:

- Kafka or WebSocket ingestion adapters
- PostgreSQL / TimescaleDB persistence
- Replay pipelines for missed events
- Retention policies and candle compaction
- Metrics export to Prometheus / Grafana
