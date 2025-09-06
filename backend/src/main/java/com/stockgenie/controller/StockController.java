package com.stockgenie.controller;

import com.stockgenie.dto.StockDataDto;
import com.stockgenie.service.FinancialDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    @Autowired
    private FinancialDataService financialDataService;

    @GetMapping("/{symbol}")
    public ResponseEntity<List<StockDataDto>> getStockData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "30") int days) {
        try {
            List<StockDataDto> stockData = financialDataService.getStockDataForRange(symbol, days);
            return ResponseEntity.ok(stockData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{symbol}/latest")
    public ResponseEntity<StockDataDto> getLatestStockData(@PathVariable String symbol) {
        try {
            StockDataDto latestData = financialDataService.getLatestStockData(symbol);
            if (latestData != null) {
                return ResponseEntity.ok(latestData);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{symbol}/refresh")
    public ResponseEntity<String> refreshStockData(@PathVariable String symbol) {
        try {
            financialDataService.refreshStockData(symbol);
            return ResponseEntity.ok("Stock data refreshed successfully for " + symbol);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to refresh stock data: " + e.getMessage());
        }
    }
}
