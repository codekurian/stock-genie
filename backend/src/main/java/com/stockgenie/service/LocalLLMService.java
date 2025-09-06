package com.stockgenie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockgenie.config.LocalLLMConfig;
import com.stockgenie.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalLLMService {
    
    private final LocalLLMConfig localLLMConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private FinancialDataService financialDataService;
    
    @Autowired
    private TechnicalAnalysisService technicalAnalysisService;
    
    /**
     * Analyze stock data using local LLM
     */
    @Cacheable(value = "llmAnalysis", key = "#request.symbol + '_' + #request.analysisType + '_' + #request.stockData.hashCode()")
    public LLMResponseDto analyzeStockData(LLMRequestDto request) {
        log.info("Analyzing stock data for {} using LLM", request.getSymbol());
        
        long startTime = System.currentTimeMillis();
        
        try {
            String prompt = buildPrompt(request);
            String response = callOllamaAPI(prompt);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return LLMResponseDto.builder()
                    .response(response)
                    .model(localLLMConfig.getModel())
                    .timestamp(LocalDateTime.now())
                    .processingTimeMs(processingTime)
                    .analysisType(request.getAnalysisType())
                    .symbol(request.getSymbol())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error analyzing stock data with LLM: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze stock data with LLM", e);
        }
    }
    
    /**
     * Simple stock analysis that fetches data internally
     */
    public Map<String, Object> analyzeStockSimple(String symbol, String analysisType, int days, boolean includeTechnical) {
        log.info("Simple analysis for {} - type: {}, days: {}, technical: {}", symbol, analysisType, days, includeTechnical);
        
        try {
            // Fetch stock data
            List<StockDataDto> stockData = financialDataService.getStockDataForRange(symbol, days);
            
            // Build simple prompt
            String prompt = buildSimplePrompt(symbol, stockData, analysisType);
            
            // Call LLM
            String response = callOllamaAPI(prompt);
            
            // Parse response for recommendation
            String recommendation = extractRecommendation(response);
            int confidence = extractConfidence(response);
            
            Map<String, Object> result = new HashMap<>();
            result.put("symbol", symbol);
            result.put("analysis", response);
            result.put("recommendation", recommendation);
            result.put("confidence", confidence);
            result.put("timestamp", LocalDateTime.now().toString());
            result.put("analysisType", analysisType);
            result.put("days", days);
            
            if (includeTechnical) {
                try {
                    Map<String, List<TechnicalAnalysisDto>> technicalData = technicalAnalysisService.calculateTechnicalIndicators(symbol, days, null);
                    result.put("technicalSummary", formatTechnicalSummary(technicalData));
                } catch (Exception e) {
                    log.warn("Failed to include technical analysis: {}", e.getMessage());
                    result.put("technicalSummary", "Technical analysis not available");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error in simple stock analysis: {}", e.getMessage());
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("symbol", symbol);
            errorResult.put("analysis", "Analysis failed: " + e.getMessage());
            errorResult.put("recommendation", "HOLD");
            errorResult.put("confidence", 0);
            errorResult.put("timestamp", LocalDateTime.now().toString());
            return errorResult;
        }
    }
    
    /**
     * Build prompt based on analysis type
     */
    private String buildPrompt(LLMRequestDto request) {
        String basePrompt = getBasePrompt(request.getAnalysisType());
        
        // Format stock data
        String stockDataText = formatStockData(request.getStockData());
        
        // Format technical indicators
        String indicatorsText = formatTechnicalIndicators(request.getTechnicalIndicators());
        
        // Replace placeholders
        return basePrompt
                .replace("{SYMBOL}", request.getSymbol())
                .replace("{STOCK_DATA}", stockDataText)
                .replace("{INDICATORS}", indicatorsText)
                .replace("{CUSTOM_PROMPT}", request.getCustomPrompt() != null ? request.getCustomPrompt() : "");
    }
    
    /**
     * Get base prompt template based on analysis type
     */
    private String getBasePrompt(String analysisType) {
        switch (analysisType) {
            case "stock-analysis":
                return """
                    You are a financial analyst. Analyze the following stock data for {SYMBOL}:
                    
                    Stock Data:
                    {STOCK_DATA}
                    
                    Technical Indicators:
                    {INDICATORS}
                    
                    Provide analysis on:
                    1. Price trends and patterns
                    2. Technical signal interpretation
                    3. Risk assessment
                    4. Buy/Sell/Hold recommendation with reasoning
                    
                    Keep response concise and actionable. Focus on the provided data only.
                    """;
                    
            case "sentiment-analysis":
                return """
                    As a specialized financial analyst, evaluate this stock data for {SYMBOL}:
                    
                    {STOCK_DATA}
                    
                    Focus on:
                    - Market sentiment indicators
                    - Volume analysis
                    - Price momentum
                    - Risk factors
                    - Investment recommendation
                    
                    Provide a sentiment-based analysis.
                    """;
                    
            case "technical-analysis":
                return """
                    Analyze the following technical indicators for {SYMBOL}:
                    
                    {INDICATORS}
                    
                    Provide:
                    1. Signal interpretation
                    2. Trend confirmation
                    3. Entry/exit points
                    4. Risk levels
                    5. Trading recommendation
                    """;
                    
            case "custom":
                return "{CUSTOM_PROMPT}\n\nStock Data:\n{STOCK_DATA}\n\nTechnical Indicators:\n{INDICATORS}";
                
            default:
                return """
                    Analyze the following stock data for {SYMBOL}:
                    
                    {STOCK_DATA}
                    
                    Technical Indicators:
                    {INDICATORS}
                    
                    Provide a comprehensive analysis.
                    """;
        }
    }
    
    /**
     * Format stock data for LLM consumption
     */
    private String formatStockData(List<StockDataDto> stockData) {
        if (stockData == null || stockData.isEmpty()) {
            return "No stock data available";
        }
        
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Show last 10 days of data
        int startIndex = Math.max(0, stockData.size() - 10);
        List<StockDataDto> recentData = stockData.subList(startIndex, stockData.size());
        
        sb.append("Recent Stock Data:\n");
        sb.append("Date\t\tOpen\tHigh\tLow\tClose\tVolume\n");
        sb.append("--------------------------------------------------------\n");
        
        for (StockDataDto data : recentData) {
            sb.append(String.format("%s\t%.2f\t%.2f\t%.2f\t%.2f\t%d\n",
                    data.getDate().format(formatter),
                    data.getOpen().doubleValue(),
                    data.getHigh().doubleValue(),
                    data.getLow().doubleValue(),
                    data.getClose().doubleValue(),
                    data.getVolume()));
        }
        
        // Add summary statistics
        if (stockData.size() > 1) {
            StockDataDto latest = stockData.get(stockData.size() - 1);
            StockDataDto previous = stockData.get(stockData.size() - 2);
            
            double change = latest.getClose().doubleValue() - previous.getClose().doubleValue();
            double changePercent = (change / previous.getClose().doubleValue()) * 100;
            
            sb.append(String.format("\nLatest Close: $%.2f (%.2f%% change from previous day)\n",
                    latest.getClose().doubleValue(), changePercent));
        }
        
        return sb.toString();
    }
    
    /**
     * Format technical indicators for LLM consumption
     */
    private String formatTechnicalIndicators(Map<String, Object> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return "No technical indicators available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Technical Indicators:\n");
        
        for (Map.Entry<String, Object> entry : indicators.entrySet()) {
            sb.append(String.format("- %s: %s\n", entry.getKey(), entry.getValue()));
        }
        
        return sb.toString();
    }
    
    /**
     * Call Ollama API
     */
    private String callOllamaAPI(String prompt) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(localLLMConfig.getBaseUrl()).build();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", localLLMConfig.getModel());
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("options", Map.of(
                    "temperature", localLLMConfig.getTemperature(),
                    "top_p", localLLMConfig.getTopP(),
                    "num_predict", localLLMConfig.getMaxTokens()
            ));
            
            String response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null) {
                throw new RuntimeException("Empty response from Ollama API");
            }
            
            // Parse JSON response
            JsonNode jsonResponse = objectMapper.readTree(response);
            String generatedText = jsonResponse.get("response").asText();
            
            log.info("LLM response received, length: {} characters", generatedText.length());
            return generatedText;
            
        } catch (WebClientResponseException e) {
            log.error("Error calling Ollama API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to call Ollama API: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling Ollama API", e);
            throw new RuntimeException("Unexpected error calling Ollama API", e);
        }
    }
    
    /**
     * Test LLM connection
     */
    public boolean testConnection() {
        try {
            String testPrompt = "Hello, are you working? Please respond with 'Yes, I am working correctly.'";
            String response = callOllamaAPI(testPrompt);
            
            log.info("LLM connection test successful: {}", response.substring(0, Math.min(100, response.length())));
            return true;
            
        } catch (Exception e) {
            log.error("LLM connection test failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get available models from Ollama
     */
    public List<String> getAvailableModels() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(localLLMConfig.getBaseUrl()).build();
            
            String response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response == null) {
                return List.of();
            }
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            JsonNode models = jsonResponse.get("models");
            
            if (models != null && models.isArray()) {
                return models.findValues("name").stream()
                        .map(JsonNode::asText)
                        .toList();
            }
            
            return List.of();
            
        } catch (Exception e) {
            log.error("Error getting available models: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Generate a quick stock analysis
     */
    public String generateQuickAnalysis(String symbol, List<StockDataDto> stockData) {
        LLMRequestDto request = LLMRequestDto.builder()
                .symbol(symbol)
                .stockData(stockData)
                .analysisType("stock-analysis")
                .build();
        
        LLMResponseDto response = analyzeStockData(request);
        return response.getResponse();
    }
    
    /**
     * Check if LLM service is available
     */
    public boolean isLLMAvailable() {
        try {
            WebClient webClient = webClientBuilder.baseUrl(localLLMConfig.getBaseUrl()).build();
            
            // Try to get models list to check if Ollama is running
            String response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(5))
                    .block();
            
            return response != null && response.contains("models");
        } catch (Exception e) {
            log.warn("LLM service not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Test LLM with a simple prompt
     */
    public String testLLM(String prompt) {
        try {
            if (!isLLMAvailable()) {
                return "LLM service is not available. Please ensure Ollama is running on " + localLLMConfig.getBaseUrl();
            }
            
            WebClient webClient = webClientBuilder.baseUrl(localLLMConfig.getBaseUrl()).build();
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", localLLMConfig.getModel());
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            
            String response = webClient.post()
                    .uri("/api/generate")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();
            
            if (response != null) {
                JsonNode jsonResponse = objectMapper.readTree(response);
                if (jsonResponse.has("response")) {
                    return jsonResponse.get("response").asText();
                }
            }
            
            return "No response received from LLM";
        } catch (Exception e) {
            log.error("Error testing LLM: {}", e.getMessage());
            return "Error testing LLM: " + e.getMessage();
        }
    }
    
    /**
     * Build simple prompt for stock analysis
     */
    private String buildSimplePrompt(String symbol, List<StockDataDto> stockData, String analysisType) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial analyst. Analyze the following stock data for ").append(symbol).append(":\n\n");
        
        prompt.append("Stock Data (last ").append(stockData.size()).append(" days):\n");
        for (StockDataDto data : stockData) {
            prompt.append(String.format("Date: %s, Open: $%.2f, High: $%.2f, Low: $%.2f, Close: $%.2f, Volume: %d\n",
                data.getDate(), data.getOpen(), data.getHigh(), data.getLow(), data.getClose(), data.getVolume()));
        }
        
        prompt.append("\nPlease provide:\n");
        prompt.append("1. Brief trend analysis\n");
        prompt.append("2. Key price movements\n");
        prompt.append("3. Volume analysis\n");
        prompt.append("4. Recommendation: BUY, SELL, or HOLD with confidence percentage (1-100)\n");
        prompt.append("5. Brief reasoning for your recommendation\n\n");
        prompt.append("Keep your analysis concise and professional.");
        
        return prompt.toString();
    }
    
    /**
     * Extract recommendation from LLM response
     */
    private String extractRecommendation(String response) {
        String upperResponse = response.toUpperCase();
        if (upperResponse.contains("BUY")) {
            return "BUY";
        } else if (upperResponse.contains("SELL")) {
            return "SELL";
        } else {
            return "HOLD";
        }
    }
    
    /**
     * Extract confidence from LLM response
     */
    private int extractConfidence(String response) {
        // Look for percentage patterns
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)%");
        java.util.regex.Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // Fallback
            }
        }
        
        // Look for confidence patterns
        if (response.toLowerCase().contains("high confidence") || response.toLowerCase().contains("very confident")) {
            return 85;
        } else if (response.toLowerCase().contains("medium confidence") || response.toLowerCase().contains("moderate")) {
            return 65;
        } else if (response.toLowerCase().contains("low confidence") || response.toLowerCase().contains("uncertain")) {
            return 35;
        }
        
        return 50; // Default confidence
    }
    
    /**
     * Format technical summary
     */
    private String formatTechnicalSummary(Map<String, List<TechnicalAnalysisDto>> technicalData) {
        if (technicalData == null || technicalData.isEmpty()) {
            return "No technical indicators available";
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Technical Indicators Summary:\n");
        
        for (Map.Entry<String, List<TechnicalAnalysisDto>> entry : technicalData.entrySet()) {
            String indicator = entry.getKey();
            List<TechnicalAnalysisDto> values = entry.getValue();
            
            if (!values.isEmpty()) {
                TechnicalAnalysisDto latest = values.get(values.size() - 1);
                summary.append(String.format("- %s: %.4f\n", indicator, latest.getValue()));
            }
        }
        
        return summary.toString();
    }
}
