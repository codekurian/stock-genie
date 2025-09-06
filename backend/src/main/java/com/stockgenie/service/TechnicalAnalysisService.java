package com.stockgenie.service;

import com.stockgenie.dto.StockDataDto;
import com.stockgenie.dto.TechnicalAnalysisDto;
import com.stockgenie.entity.TechnicalAnalysis;
import com.stockgenie.repository.TechnicalAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TechnicalAnalysisService {
    
    private final TechnicalAnalysisRepository technicalAnalysisRepository;
    
    /**
     * Calculate technical indicators for a list of stock data
     */
    @Cacheable(value = "technicalAnalysis", key = "#symbol + '_' + #indicators.hashCode()")
    public Map<String, List<TechnicalAnalysisDto>> calculateIndicators(String symbol, List<StockDataDto> stockData, List<String> indicators) {
        log.info("Calculating technical indicators for {}: {}", symbol, indicators);
        
        if (stockData == null || stockData.isEmpty()) {
            log.warn("No stock data provided for technical analysis");
            return new HashMap<>();
        }
        
        Map<String, List<TechnicalAnalysisDto>> results = new HashMap<>();
        
        for (String indicator : indicators) {
            try {
                List<TechnicalAnalysisDto> indicatorResults = calculateIndicator(symbol, stockData, indicator);
                results.put(indicator, indicatorResults);
                
                // Save to database
                saveTechnicalAnalysisToDatabase(indicatorResults);
                
            } catch (Exception e) {
                log.error("Error calculating indicator {} for {}: {}", indicator, symbol, e.getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * Calculate a specific indicator
     */
    private List<TechnicalAnalysisDto> calculateIndicator(String symbol, List<StockDataDto> stockData, String indicator) {
        switch (indicator.toUpperCase()) {
            case "SMA_20":
                return calculateSMA(symbol, stockData, 20);
            case "SMA_50":
                return calculateSMA(symbol, stockData, 50);
            case "EMA_12":
                return calculateEMA(symbol, stockData, 12);
            case "EMA_26":
                return calculateEMA(symbol, stockData, 26);
            case "RSI_14":
                return calculateRSI(symbol, stockData, 14);
            case "MACD":
                return calculateMACD(symbol, stockData);
            case "OBV":
                return calculateOBV(symbol, stockData);
            default:
                log.warn("Unknown indicator: {}", indicator);
                return new ArrayList<>();
        }
    }
    
    /**
     * Calculate Simple Moving Average
     */
    private List<TechnicalAnalysisDto> calculateSMA(String symbol, List<StockDataDto> stockData, int period) {
        List<TechnicalAnalysisDto> results = new ArrayList<>();
        
        for (int i = period - 1; i < stockData.size(); i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = i - period + 1; j <= i; j++) {
                sum = sum.add(stockData.get(j).getClose());
            }
            BigDecimal sma = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            
            TechnicalAnalysisDto analysis = TechnicalAnalysisDto.builder()
                    .symbol(symbol)
                    .date(stockData.get(i).getDate())
                    .indicatorType(TechnicalAnalysis.IndicatorType.SMA)
                    .period(period)
                    .value(sma)
                    .build();
            
            results.add(analysis);
        }
        
        return results;
    }
    
    /**
     * Calculate Exponential Moving Average
     */
    private List<TechnicalAnalysisDto> calculateEMA(String symbol, List<StockDataDto> stockData, int period) {
        List<TechnicalAnalysisDto> results = new ArrayList<>();
        
        if (stockData.size() < period) {
            return results;
        }
        
        // Calculate multiplier
        BigDecimal multiplier = BigDecimal.valueOf(2.0).divide(BigDecimal.valueOf(period + 1), 6, RoundingMode.HALF_UP);
        
        // First EMA is SMA
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            sum = sum.add(stockData.get(i).getClose());
        }
        BigDecimal ema = sum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        
        TechnicalAnalysisDto firstAnalysis = TechnicalAnalysisDto.builder()
                .symbol(symbol)
                .date(stockData.get(period - 1).getDate())
                .indicatorType(TechnicalAnalysis.IndicatorType.EMA)
                .period(period)
                .value(ema)
                .build();
        results.add(firstAnalysis);
        
        // Calculate subsequent EMAs
        for (int i = period; i < stockData.size(); i++) {
            BigDecimal currentPrice = stockData.get(i).getClose();
            ema = currentPrice.multiply(multiplier).add(ema.multiply(BigDecimal.ONE.subtract(multiplier)));
            
            TechnicalAnalysisDto analysis = TechnicalAnalysisDto.builder()
                    .symbol(symbol)
                    .date(stockData.get(i).getDate())
                    .indicatorType(TechnicalAnalysis.IndicatorType.EMA)
                    .period(period)
                    .value(ema.setScale(4, RoundingMode.HALF_UP))
                    .build();
            
            results.add(analysis);
        }
        
        return results;
    }
    
    /**
     * Calculate Relative Strength Index
     */
    private List<TechnicalAnalysisDto> calculateRSI(String symbol, List<StockDataDto> stockData, int period) {
        List<TechnicalAnalysisDto> results = new ArrayList<>();
        
        if (stockData.size() < period + 1) {
            return results;
        }
        
        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();
        
        // Calculate price changes
        for (int i = 1; i < stockData.size(); i++) {
            BigDecimal change = stockData.get(i).getClose().subtract(stockData.get(i - 1).getClose());
            gains.add(change.compareTo(BigDecimal.ZERO) > 0 ? change : BigDecimal.ZERO);
            losses.add(change.compareTo(BigDecimal.ZERO) < 0 ? change.abs() : BigDecimal.ZERO);
        }
        
        // Calculate initial average gain and loss
        BigDecimal avgGain = gains.subList(0, period).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        
        BigDecimal avgLoss = losses.subList(0, period).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        
        // Calculate RSI
        for (int i = period; i < gains.size(); i++) {
            // Update averages using Wilder's smoothing
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gains.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(losses.get(i))
                    .divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
            
            BigDecimal rs = avgLoss.compareTo(BigDecimal.ZERO) == 0 ? 
                    BigDecimal.valueOf(100) : avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
            BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                    BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 4, RoundingMode.HALF_UP));
            
            TechnicalAnalysisDto analysis = TechnicalAnalysisDto.builder()
                    .symbol(symbol)
                    .date(stockData.get(i + 1).getDate())
                    .indicatorType(TechnicalAnalysis.IndicatorType.RSI)
                    .period(period)
                    .value(rsi)
                    .build();
            
            results.add(analysis);
        }
        
        return results;
    }
    
    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     */
    private List<TechnicalAnalysisDto> calculateMACD(String symbol, List<StockDataDto> stockData) {
        List<TechnicalAnalysisDto> results = new ArrayList<>();
        
        if (stockData.size() < 26) {
            return results;
        }
        
        // Calculate EMAs
        List<TechnicalAnalysisDto> ema12 = calculateEMA(symbol, stockData, 12);
        List<TechnicalAnalysisDto> ema26 = calculateEMA(symbol, stockData, 26);
        
        // Calculate MACD line
        for (int i = 0; i < Math.min(ema12.size(), ema26.size()); i++) {
            BigDecimal macdLine = ema12.get(i).getValue().subtract(ema26.get(i).getValue());
            
            TechnicalAnalysisDto analysis = TechnicalAnalysisDto.builder()
                    .symbol(symbol)
                    .date(ema12.get(i).getDate())
                    .indicatorType(TechnicalAnalysis.IndicatorType.MACD)
                    .period(12)
                    .value(macdLine)
                    .build();
            
            results.add(analysis);
        }
        
        return results;
    }
    
    /**
     * Calculate On-Balance Volume
     */
    private List<TechnicalAnalysisDto> calculateOBV(String symbol, List<StockDataDto> stockData) {
        List<TechnicalAnalysisDto> results = new ArrayList<>();
        
        if (stockData.size() < 2) {
            return results;
        }
        
        BigDecimal obv = BigDecimal.ZERO;
        
        for (int i = 1; i < stockData.size(); i++) {
            BigDecimal currentClose = stockData.get(i).getClose();
            BigDecimal previousClose = stockData.get(i - 1).getClose();
            BigDecimal volume = BigDecimal.valueOf(stockData.get(i).getVolume());
            
            if (currentClose.compareTo(previousClose) > 0) {
                obv = obv.add(volume);
            } else if (currentClose.compareTo(previousClose) < 0) {
                obv = obv.subtract(volume);
            }
            // If close prices are equal, OBV remains unchanged
            
            TechnicalAnalysisDto analysis = TechnicalAnalysisDto.builder()
                    .symbol(symbol)
                    .date(stockData.get(i).getDate())
                    .indicatorType(TechnicalAnalysis.IndicatorType.SMA) // Using SMA as placeholder
                    .period(1)
                    .value(obv)
                    .build();
            
            results.add(analysis);
        }
        
        return results;
    }
    
    /**
     * Save technical analysis results to database
     */
    private void saveTechnicalAnalysisToDatabase(List<TechnicalAnalysisDto> analysisList) {
        List<TechnicalAnalysis> entities = analysisList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        
        technicalAnalysisRepository.saveAll(entities);
        log.info("Saved {} technical analysis records to database", entities.size());
    }
    
    /**
     * Convert DTO to Entity
     */
    private TechnicalAnalysis convertToEntity(TechnicalAnalysisDto dto) {
        return TechnicalAnalysis.builder()
                .symbol(dto.getSymbol())
                .date(dto.getDate())
                .indicatorType(dto.getIndicatorType())
                .period(dto.getPeriod())
                .value(dto.getValue())
                .signal(dto.getSignal())
                .histogram(dto.getHistogram())
                .metadata(dto.getMetadata())
                .build();
    }
    
    /**
     * Get technical analysis from database
     */
    public List<TechnicalAnalysisDto> getTechnicalAnalysis(String symbol, TechnicalAnalysis.IndicatorType indicatorType, LocalDate startDate, LocalDate endDate) {
        List<TechnicalAnalysis> analysisList = technicalAnalysisRepository
                .findTechnicalAnalysisInRange(symbol, indicatorType, startDate, endDate);
        
        return analysisList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Convert Entity to DTO
     */
    private TechnicalAnalysisDto convertToDto(TechnicalAnalysis entity) {
        return TechnicalAnalysisDto.builder()
                .symbol(entity.getSymbol())
                .date(entity.getDate())
                .indicatorType(entity.getIndicatorType())
                .period(entity.getPeriod())
                .value(entity.getValue())
                .signal(entity.getSignal())
                .histogram(entity.getHistogram())
                .metadata(entity.getMetadata())
                .build();
    }
    
    /**
     * Generate trading signals based on technical indicators
     */
    public Map<String, String> generateSignals(String symbol, List<StockDataDto> stockData) {
        Map<String, String> signals = new HashMap<>();
        
        if (stockData.size() < 50) {
            log.warn("Insufficient data for signal generation: {} records", stockData.size());
            return signals;
        }
        
        // Calculate indicators
        List<String> indicators = Arrays.asList("SMA_20", "SMA_50", "RSI_14", "MACD");
        Map<String, List<TechnicalAnalysisDto>> analysisResults = calculateIndicators(symbol, stockData, indicators);
        
        // Get latest values
        StockDataDto latestData = stockData.get(stockData.size() - 1);
        
        // SMA Signal
        if (analysisResults.containsKey("SMA_20") && analysisResults.containsKey("SMA_50")) {
            List<TechnicalAnalysisDto> sma20 = analysisResults.get("SMA_20");
            List<TechnicalAnalysisDto> sma50 = analysisResults.get("SMA_50");
            
            if (!sma20.isEmpty() && !sma50.isEmpty()) {
                BigDecimal sma20Value = sma20.get(sma20.size() - 1).getValue();
                BigDecimal sma50Value = sma50.get(sma50.size() - 1).getValue();
                BigDecimal currentPrice = latestData.getClose();
                
                if (sma20Value.compareTo(sma50Value) > 0 && currentPrice.compareTo(sma20Value) > 0) {
                    signals.put("SMA", "BUY");
                } else if (sma20Value.compareTo(sma50Value) < 0 && currentPrice.compareTo(sma20Value) < 0) {
                    signals.put("SMA", "SELL");
                } else {
                    signals.put("SMA", "HOLD");
                }
            }
        }
        
        // RSI Signal
        if (analysisResults.containsKey("RSI_14")) {
            List<TechnicalAnalysisDto> rsi = analysisResults.get("RSI_14");
            if (!rsi.isEmpty()) {
                BigDecimal rsiValue = rsi.get(rsi.size() - 1).getValue();
                
                if (rsiValue.compareTo(new BigDecimal("70")) > 0) {
                    signals.put("RSI", "SELL");
                } else if (rsiValue.compareTo(new BigDecimal("30")) < 0) {
                    signals.put("RSI", "BUY");
                } else {
                    signals.put("RSI", "HOLD");
                }
            }
        }
        
        // MACD Signal
        if (analysisResults.containsKey("MACD")) {
            List<TechnicalAnalysisDto> macd = analysisResults.get("MACD");
            if (!macd.isEmpty()) {
                TechnicalAnalysisDto latestMacd = macd.get(macd.size() - 1);
                BigDecimal macdValue = latestMacd.getValue();
                
                if (macdValue.compareTo(BigDecimal.ZERO) > 0) {
                    signals.put("MACD", "BUY");
                } else {
                    signals.put("MACD", "SELL");
                }
            }
        }
        
        // Overall signal
        long buySignals = signals.values().stream().mapToLong(s -> "BUY".equals(s) ? 1 : 0).sum();
        long sellSignals = signals.values().stream().mapToLong(s -> "SELL".equals(s) ? 1 : 0).sum();
        
        if (buySignals > sellSignals) {
            signals.put("OVERALL", "BUY");
        } else if (sellSignals > buySignals) {
            signals.put("OVERALL", "SELL");
        } else {
            signals.put("OVERALL", "HOLD");
        }
        
        return signals;
    }
    
    /**
     * Get available indicators list
     */
    public Map<String, Object> getAvailableIndicators() {
        Map<String, Object> indicators = new HashMap<>();
        indicators.put("availableIndicators", List.of(
            "SMA_20", "SMA_50", "EMA_12", "EMA_26", "RSI_14", "MACD", "OBV"
        ));
        indicators.put("descriptions", Map.of(
            "SMA_20", "Simple Moving Average (20 periods)",
            "SMA_50", "Simple Moving Average (50 periods)",
            "EMA_12", "Exponential Moving Average (12 periods)",
            "EMA_26", "Exponential Moving Average (26 periods)",
            "RSI_14", "Relative Strength Index (14 periods)",
            "MACD", "Moving Average Convergence Divergence",
            "OBV", "On-Balance Volume"
        ));
        return indicators;
    }
    
    /**
     * Calculate technical indicators for a symbol with days parameter
     */
    public Map<String, List<TechnicalAnalysisDto>> calculateTechnicalIndicators(String symbol, int days, List<String> indicators) {
        // This would need to fetch stock data first, but for now return available indicators
        Map<String, Object> available = getAvailableIndicators();
        Map<String, List<TechnicalAnalysisDto>> results = new HashMap<>();
        
        // For now, return empty results with available indicators info
        @SuppressWarnings("unchecked")
        List<String> availableList = (List<String>) available.get("availableIndicators");
        
        if (indicators == null || indicators.isEmpty()) {
            indicators = availableList;
        }
        
        for (String indicator : indicators) {
            if (availableList.contains(indicator)) {
                results.put(indicator, new ArrayList<>());
            }
        }
        
        return results;
    }
}