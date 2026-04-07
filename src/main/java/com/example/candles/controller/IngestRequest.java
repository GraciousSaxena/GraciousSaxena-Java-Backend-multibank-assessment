package com.example.candles.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record IngestRequest(
        @NotBlank String symbol,
        @DecimalMin(value = "0.0", inclusive = false) double bid,
        @DecimalMin(value = "0.0", inclusive = false) double ask,
        @PositiveOrZero long timestamp
) {
}
