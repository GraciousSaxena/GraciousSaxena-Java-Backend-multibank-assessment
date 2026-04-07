package com.example.candles.controller;

import com.example.candles.CandleAggregationApplication;
import com.example.candles.model.BidAskEvent;
import com.example.candles.service.CandleAggregationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CandleAggregationApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "market.simulator.enabled=false")
class CandleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CandleAggregationService service;

    @BeforeEach
    void setUp() {
        service.clear();
        service.process(new BidAskEvent("BTC-USD", 100.0, 102.0, 100));
        service.process(new BidAskEvent("BTC-USD", 103.0, 105.0, 100));
    }

    @Test
    void shouldReturnHistoryArrays() throws Exception {
        mockMvc.perform(get("/history")
                        .param("symbol", "BTC-USD")
                        .param("interval", "1s")
                        .param("from", "100")
                        .param("to", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s").value("ok"))
                .andExpect(jsonPath("$.t[0]").value(100))
                .andExpect(jsonPath("$.o[0]").value(101.0))
                .andExpect(jsonPath("$.h[0]").value(104.0))
                .andExpect(jsonPath("$.l[0]").value(101.0))
                .andExpect(jsonPath("$.c[0]").value(104.0))
                .andExpect(jsonPath("$.v[0]").value(2));
    }

    @Test
    void shouldReturnNoDataWhenMissing() throws Exception {
        mockMvc.perform(get("/history")
                        .param("symbol", "ETH-USD")
                        .param("interval", "1s")
                        .param("from", "100")
                        .param("to", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s").value("no_data"));
    }
}
