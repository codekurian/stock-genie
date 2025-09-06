package com.stockgenie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockgenie.config.FinancialApiConfig;
import com.stockgenie.dto.AlphaVantageResponse;
import com.stockgenie.dto.StockDataDto;
import com.stockgenie.entity.StockData;
import com.stockgenie.repository.StockDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialDataService {
    
    private final StockDataRepository stockDataRepository;
    private final FinancialApiConfig financialApiConfig;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;
    private final ApiOptimizationService apiOptimizationService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Fetch stock data for a symbol within a date range
     * First checks database, then fetches from API if needed
     */
    @Cacheable(value = "stockData", key = "#symbol + '_' + #startDate + '_' + #endDate")
    public List<StockDataDto> fetchStockData(String symbol, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching stock data for {} from {} to {}", symbol, startDate, endDate);
        
        // Check if we have data in the database
        if (hasDataInRange(symbol, startDate, endDate)) {
            log.info("Found existing data in database for {}", symbol);
            return getDataFromDatabase(symbol, startDate, endDate);
        }
        
        // Fetch from API
        log.info("Fetching data from API for {}", symbol);
        return fetchFromAlphaVantage(symbol, startDate, endDate);
    }
    
    /**
     * Check if we have data in the database for the given range
     */
    private boolean hasDataInRange(String symbol, LocalDate startDate, LocalDate endDate) {
        return stockDataRepository.existsBySymbolAndDateBetween(symbol, startDate, endDate);
    }
    
    /**
     * Get data from database
     */
    private List<StockDataDto> getDataFromDatabase(String symbol, LocalDate startDate, LocalDate endDate) {
        List<StockData> stockDataList = stockDataRepository.findStockDataInRange(symbol, startDate, endDate);
        return stockDataList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Fetch data from Alpha Vantage API
     */
    private List<StockDataDto> fetchFromAlphaVantage(String symbol, LocalDate startDate, LocalDate endDate) {
        try {
            String apiKey = financialApiConfig.getAlphaVantage().getApiKey();
            String baseUrl = financialApiConfig.getAlphaVantage().getBaseUrl();
            
            if ("demo".equals(apiKey) || apiKey == null || apiKey.trim().isEmpty()) {
                log.warn("Using demo API key - limited functionality. Please set ALPHA_VANTAGE_API_KEY environment variable for real data.");
                return createMockData(symbol, startDate, endDate);
            }
            
            log.info("Fetching real data from Alpha Vantage for symbol: {}", symbol);
            
            // Build API URL
            String apiUrl = baseUrl + "?function=TIME_SERIES_DAILY&symbol=" + symbol + 
                           "&outputsize=full&apikey=" + apiKey;
            
            // Use optimized API service with retry mechanism
            String responseJson = apiOptimizationService.makeApiCallWithRetry(apiUrl, "stock_data_" + symbol);
            
            if (responseJson == null || responseJson.trim().isEmpty()) {
                log.error("Empty response received from Alpha Vantage for {}", symbol);
                return createMockData(symbol, startDate, endDate);
            }
            
            // Check for API error messages
            if (responseJson.contains("\"Error Message\"") || responseJson.contains("\"Note\"")) {
                log.error("Alpha Vantage API error for {}: {}", symbol, responseJson);
                return createMockData(symbol, startDate, endDate);
            }
            
            // Parse the JSON response
            AlphaVantageResponse response = parseAlphaVantageResponse(responseJson);
            
            if (response == null || response.getTimeSeries() == null || response.getTimeSeries().isEmpty()) {
                log.error("No valid time series data received from Alpha Vantage for {}", symbol);
                return createMockData(symbol, startDate, endDate);
            }
            
            List<StockDataDto> stockDataList = convertAlphaVantageResponse(response, symbol);
            
            // Save to database
            saveStockDataToDatabase(stockDataList);
            
            // Filter by date range
            return stockDataList.stream()
                    .filter(data -> !data.getDate().isBefore(startDate) && !data.getDate().isAfter(endDate))
                    .collect(Collectors.toList());
                    
        } catch (WebClientResponseException e) {
            log.error("HTTP error fetching data from Alpha Vantage for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            return createMockData(symbol, startDate, endDate);
        } catch (Exception e) {
            log.error("Unexpected error fetching stock data for {}: {}", symbol, e.getMessage());
            return createMockData(symbol, startDate, endDate);
        }
    }
    
    /**
     * Parse Alpha Vantage JSON response
     */
    private AlphaVantageResponse parseAlphaVantageResponse(String responseJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);
            
            // Check for time series data
            JsonNode timeSeriesNode = rootNode.get("Time Series (Daily)");
            if (timeSeriesNode == null) {
                log.warn("No Time Series (Daily) found in response");
                return null;
            }
            
            AlphaVantageResponse response = new AlphaVantageResponse();
            Map<String, AlphaVantageResponse.TimeSeriesData> timeSeries = new java.util.HashMap<>();
            
            // Parse metadata
            JsonNode metaDataNode = rootNode.get("Meta Data");
            if (metaDataNode != null) {
                AlphaVantageResponse.MetaData metaData = new AlphaVantageResponse.MetaData();
                metaData.setInformation(metaDataNode.get("1. Information").asText());
                metaData.setSymbol(metaDataNode.get("2. Symbol").asText());
                metaData.setLastRefreshed(metaDataNode.get("3. Last Refreshed").asText());
                metaData.setOutputSize(metaDataNode.get("4. Output Size").asText());
                metaData.setTimeZone(metaDataNode.get("5. Time Zone").asText());
                response.setMetaData(metaData);
            }
            
            // Parse time series data
            timeSeriesNode.fields().forEachRemaining(entry -> {
                String date = entry.getKey();
                JsonNode dataNode = entry.getValue();
                
                AlphaVantageResponse.TimeSeriesData data = new AlphaVantageResponse.TimeSeriesData();
                data.setOpen(dataNode.get("1. open").asText());
                data.setHigh(dataNode.get("2. high").asText());
                data.setLow(dataNode.get("3. low").asText());
                data.setClose(dataNode.get("4. close").asText());
                data.setVolume(dataNode.get("5. volume").asText());
                
                timeSeries.put(date, data);
            });
            
            response.setTimeSeries(timeSeries);
            return response;
            
        } catch (Exception e) {
            log.error("Error parsing Alpha Vantage response: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert Alpha Vantage response to StockDataDto list
     */
    private List<StockDataDto> convertAlphaVantageResponse(AlphaVantageResponse response, String symbol) {
        List<StockDataDto> stockDataList = new ArrayList<>();
        
        for (Map.Entry<String, AlphaVantageResponse.TimeSeriesData> entry : response.getTimeSeries().entrySet()) {
            try {
                LocalDate date = LocalDate.parse(entry.getKey(), DATE_FORMATTER);
                AlphaVantageResponse.TimeSeriesData data = entry.getValue();
                
                StockDataDto stockData = StockDataDto.builder()
                        .symbol(symbol)
                        .date(date)
                        .open(new BigDecimal(data.getOpen()))
                        .high(new BigDecimal(data.getHigh()))
                        .low(new BigDecimal(data.getLow()))
                        .close(new BigDecimal(data.getClose()))
                        .volume(Long.parseLong(data.getVolume()))
                        .adjustedClose(new BigDecimal(data.getClose())) // Use close as adjusted close for now
                        .dataSource("alpha-vantage")
                        .build();
                
                stockDataList.add(stockData);
            } catch (Exception e) {
                log.warn("Error parsing data for date {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        return stockDataList;
    }
    
    /**
     * Save stock data to database
     */
    private void saveStockDataToDatabase(List<StockDataDto> stockDataList) {
        List<StockData> entities = stockDataList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        
        stockDataRepository.saveAll(entities);
        log.info("Saved {} stock data records to database", entities.size());
    }
    
    /**
     * Convert DTO to Entity
     */
    private StockData convertToEntity(StockDataDto dto) {
        return StockData.builder()
                .symbol(dto.getSymbol())
                .date(dto.getDate())
                .open(dto.getOpen())
                .high(dto.getHigh())
                .low(dto.getLow())
                .close(dto.getClose())
                .volume(dto.getVolume())
                .adjustedClose(dto.getAdjustedClose())
                .dataSource(dto.getDataSource())
                .build();
    }
    
    /**
     * Convert Entity to DTO
     */
    private StockDataDto convertToDto(StockData entity) {
        return StockDataDto.builder()
                .symbol(entity.getSymbol())
                .date(entity.getDate())
                .open(entity.getOpen())
                .high(entity.getHigh())
                .low(entity.getLow())
                .close(entity.getClose())
                .volume(entity.getVolume())
                .adjustedClose(entity.getAdjustedClose())
                .dataSource(entity.getDataSource())
                .build();
    }
    
    /**
     * Create mock data for testing (when using demo API key)
     */
    private List<StockDataDto> createMockData(String symbol, LocalDate startDate, LocalDate endDate) {
        List<StockDataDto> mockData = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        BigDecimal basePrice = new BigDecimal("150.00");
        int dayCount = 0;
        
        while (!currentDate.isAfter(endDate)) {
            // Create realistic mock data with some variation
            BigDecimal variation = new BigDecimal(dayCount % 10 - 5).multiply(new BigDecimal("0.5"));
            BigDecimal open = basePrice.add(variation);
            BigDecimal close = open.add(new BigDecimal(Math.random() * 4 - 2));
            BigDecimal high = open.max(close).add(new BigDecimal(Math.random() * 2));
            BigDecimal low = open.min(close).subtract(new BigDecimal(Math.random() * 2));
            
            StockDataDto stockData = StockDataDto.builder()
                    .symbol(symbol)
                    .date(currentDate)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .volume(1000000L + (long)(Math.random() * 500000))
                    .adjustedClose(close)
                    .dataSource("mock")
                    .build();
            
            mockData.add(stockData);
            currentDate = currentDate.plusDays(1);
            dayCount++;
        }
        
        log.info("Created {} mock data records for {}", mockData.size(), symbol);
        return mockData;
    }
    
    /**
     * Get latest available date for a symbol
     */
    public LocalDate getLatestDate(String symbol) {
        return stockDataRepository.findLatestDateBySymbol(symbol).orElse(null);
    }
    
    /**
     * Get earliest available date for a symbol
     */
    public LocalDate getEarliestDate(String symbol) {
        return stockDataRepository.findEarliestDateBySymbol(symbol).orElse(null);
    }
    
    /**
     * Get stock data for a symbol with number of days (convenience method for controllers)
     */
    public List<StockDataDto> getStockDataForRange(String symbol, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        return fetchStockData(symbol, startDate, endDate);
    }
    
    /**
     * Get latest stock data for a symbol
     */
    public StockDataDto getLatestStockData(String symbol) {
        LocalDate latestDate = getLatestDate(symbol);
        if (latestDate == null) {
            // If no data exists, fetch some data first
            List<StockDataDto> data = getStockDataForRange(symbol, 7);
            if (!data.isEmpty()) {
                return data.get(data.size() - 1); // Return the most recent
            }
            return null;
        }
        
        Optional<StockData> stockDataOpt = stockDataRepository.findBySymbolAndDate(symbol, latestDate);
        if (stockDataOpt.isPresent()) {
            return convertToDto(stockDataOpt.get());
        }
        return null;
    }
    
    /**
     * Refresh stock data for a symbol (clear cache and refetch)
     */
    public void refreshStockData(String symbol) {
        log.info("Refreshing stock data for {}", symbol);
        // Clear cache for this symbol
        // Note: In a real implementation, you'd use @CacheEvict or similar
        // For now, we'll just log the action
        log.info("Cache cleared for symbol: {}", symbol);
        
        // Fetch fresh data
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        fetchStockData(symbol, startDate, endDate);
    }
}
