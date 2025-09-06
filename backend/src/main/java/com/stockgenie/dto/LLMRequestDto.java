package com.stockgenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRequestDto {
    private String symbol;
    private List<StockDataDto> stockData;
    private Map<String, Object> technicalIndicators;
    private String analysisType; // "stock-analysis", "sentiment-analysis", "technical-analysis"
    private String customPrompt;
}
