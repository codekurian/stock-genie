package com.stockgenie.controller;

import com.stockgenie.dto.TechnicalAnalysisDto;
import com.stockgenie.service.TechnicalAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    @Autowired
    private TechnicalAnalysisService technicalAnalysisService;

    @GetMapping("/{symbol}/technical")
    public ResponseEntity<Map<String, Object>> getTechnicalAnalysis(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) List<String> indicators) {
        try {
            Map<String, List<TechnicalAnalysisDto>> analysis = technicalAnalysisService.calculateTechnicalIndicators(symbol, days, indicators);
            Map<String, Object> response = Map.of(
                "symbol", symbol,
                "days", days,
                "indicators", indicators != null ? indicators : "all",
                "analysis", analysis,
                "message", "Technical analysis completed successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{symbol}/indicators")
    public ResponseEntity<Map<String, Object>> getAvailableIndicators(@PathVariable String symbol) {
        try {
            Map<String, Object> indicators = technicalAnalysisService.getAvailableIndicators();
            indicators.put("symbol", symbol);
            return ResponseEntity.ok(indicators);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{symbol}/calculate")
    public ResponseEntity<Map<String, Object>> calculateIndicators(
            @PathVariable String symbol,
            @RequestBody Map<String, Object> request) {
        try {
            int days = (Integer) request.getOrDefault("days", 30);
            @SuppressWarnings("unchecked")
            List<String> indicators = (List<String>) request.getOrDefault("indicators", List.of());
            
            Map<String, List<TechnicalAnalysisDto>> analysis = technicalAnalysisService.calculateTechnicalIndicators(symbol, days, indicators);
            
            Map<String, Object> response = Map.of(
                "symbol", symbol,
                "days", days,
                "indicators", indicators,
                "analysis", analysis,
                "message", "Technical analysis calculation completed successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
