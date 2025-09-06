# 📚 Stock Genie API Documentation

## **Overview**

Stock Genie is a comprehensive stock analysis platform that provides:
- **Real-time stock data** from financial APIs
- **Technical analysis** with multiple indicators
- **AI-powered insights** using local LLM models
- **Caching** for optimal performance
- **RESTful APIs** with comprehensive documentation

## **Base Information**

- **Base URL**: `http://localhost:8080/api/v1`
- **API Version**: v1
- **Content Type**: `application/json`
- **Authentication**: None (development mode)
- **Rate Limits**: 5 requests/minute for stock data APIs

## **Swagger UI**

Interactive API documentation is available at:
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/api-docs`

---

## **1. Stock Data APIs**

### **1.1 Get Stock Data for Date Range**
**`GET /api/v1/stocks/{symbol}/range`**

**Description:** Fetches historical stock data for a given symbol within the specified date range. Data is fetched from API if not cached, otherwise from database.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol (1-5 uppercase letters) | `AAPL`, `MSFT`, `GOOGL` |

**Query Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `startDate` | date | Yes | Start date in YYYY-MM-DD format | `2024-01-01` |
| `endDate` | date | Yes | End date in YYYY-MM-DD format | `2024-01-31` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/stocks/AAPL/range?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
[
  {
    "symbol": "AAPL",
    "date": "2024-01-02",
    "open": 185.92,
    "high": 186.40,
    "low": 182.13,
    "close": 185.14,
    "volume": 52430700,
    "adjustedClose": 185.14,
    "dataSource": "alpha-vantage"
  },
  {
    "symbol": "AAPL",
    "date": "2024-01-03",
    "open": 185.14,
    "high": 186.06,
    "low": 183.45,
    "close": 185.04,
    "volume": 47311400,
    "adjustedClose": 185.04,
    "dataSource": "alpha-vantage"
  }
]
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `symbol` | string | Stock symbol |
| `date` | date | Trading date |
| `open` | number | Opening price |
| `high` | number | Highest price of the day |
| `low` | number | Lowest price of the day |
| `close` | number | Closing price |
| `volume` | integer | Trading volume |
| `adjustedClose` | number | Adjusted closing price |
| `dataSource` | string | Data source identifier |

**Status Codes:**
- `200 OK`: Stock data retrieved successfully
- `400 Bad Request`: Invalid symbol or date range
- `404 Not Found`: No data found for the symbol
- `500 Internal Server Error`: Server error

---

### **1.2 Get Latest Stock Data**
**`GET /api/v1/stocks/{symbol}/latest`**

**Description:** Fetches the most recent stock data point for a given symbol.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol | `AAPL` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/stocks/AAPL/latest" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "symbol": "AAPL",
  "date": "2024-09-05",
  "open": 150.25,
  "high": 152.80,
  "low": 149.50,
  "close": 151.75,
  "volume": 45000000,
  "adjustedClose": 151.75,
  "dataSource": "alpha-vantage"
}
```

**Status Codes:**
- `200 OK`: Latest stock data retrieved successfully
- `400 Bad Request`: Invalid symbol
- `404 Not Found`: No data found for the symbol
- `500 Internal Server Error`: Server error

---

### **1.3 Get Recent Stock Data**
**`GET /api/v1/stocks/{symbol}`**

**Description:** Fetches recent stock data for a given symbol (defaults to last 30 days).

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol | `AAPL` |

**Query Parameters:**
| Parameter | Type | Required | Default | Description | Example |
|-----------|------|----------|---------|-------------|---------|
| `days` | integer | No | 30 | Number of days to fetch (1-365) | `7` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/stocks/AAPL?days=7" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
[
  {
    "symbol": "AAPL",
    "date": "2024-08-30",
    "open": 148.50,
    "high": 150.25,
    "low": 147.80,
    "close": 149.75,
    "volume": 42000000,
    "adjustedClose": 149.75,
    "dataSource": "alpha-vantage"
  }
]
```

**Status Codes:**
- `200 OK`: Stock data retrieved successfully
- `400 Bad Request`: Invalid symbol or days parameter
- `404 Not Found`: No data found for the symbol
- `500 Internal Server Error`: Server error

---

### **1.4 Get Stock Data Availability Info**
**`GET /api/v1/stocks/{symbol}/info`**

**Description:** Returns information about data availability for a given symbol.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol | `AAPL` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/stocks/AAPL/info" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "symbol": "AAPL",
  "earliestDate": "2020-01-02",
  "latestDate": "2024-09-05",
  "totalDays": 1200,
  "dataSource": "alpha-vantage"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `symbol` | string | Stock symbol |
| `earliestDate` | date | Earliest available date |
| `latestDate` | date | Latest available date |
| `totalDays` | integer | Total number of trading days |
| `dataSource` | string | Data source identifier |

**Status Codes:**
- `200 OK`: Data availability info retrieved successfully
- `400 Bad Request`: Invalid symbol
- `404 Not Found`: No data found for the symbol
- `500 Internal Server Error`: Server error

---

## **2. Technical Analysis APIs**

### **2.1 Calculate Technical Indicators**
**`POST /api/v1/analysis/technical`**

**Description:** Calculates specified technical indicators for a stock symbol. Supports SMA, EMA, RSI, MACD, and OBV indicators.

**Request Payload:**
```json
{
  "symbol": "AAPL",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "indicators": ["SMA_20", "SMA_50", "RSI_14", "MACD"]
}
```

**Request Fields:**
| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol (1-5 uppercase letters) | `AAPL` |
| `startDate` | date | No | Start date (defaults to 30 days ago) | `2024-01-01` |
| `endDate` | date | No | End date (defaults to today) | `2024-01-31` |
| `indicators` | array | Yes | List of indicators to calculate | `["SMA_20", "RSI_14"]` |

**Available Indicators:**
| Indicator | Description | Period |
|-----------|-------------|---------|
| `SMA_20` | Simple Moving Average | 20 periods |
| `SMA_50` | Simple Moving Average | 50 periods |
| `EMA_12` | Exponential Moving Average | 12 periods |
| `EMA_26` | Exponential Moving Average | 26 periods |
| `RSI_14` | Relative Strength Index | 14 periods |
| `MACD` | Moving Average Convergence Divergence | 12,26,9 |
| `OBV` | On-Balance Volume | N/A |

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/analysis/technical" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "symbol": "AAPL",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "indicators": ["SMA_20", "RSI_14", "MACD"]
  }'
```

**Response Payload:**
```json
{
  "SMA_20": [
    {
      "symbol": "AAPL",
      "date": "2024-01-22",
      "indicatorType": "SMA",
      "period": 20,
      "value": 185.45,
      "signal": null,
      "histogram": null,
      "metadata": null
    }
  ],
  "RSI_14": [
    {
      "symbol": "AAPL",
      "date": "2024-01-22",
      "indicatorType": "RSI",
      "period": 14,
      "value": 65.23,
      "signal": null,
      "histogram": null,
      "metadata": null
    }
  ],
  "MACD": [
    {
      "symbol": "AAPL",
      "date": "2024-01-22",
      "indicatorType": "MACD",
      "period": 12,
      "value": 1.25,
      "signal": 1.18,
      "histogram": 0.07,
      "metadata": null
    }
  ]
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `symbol` | string | Stock symbol |
| `date` | date | Calculation date |
| `indicatorType` | enum | Type of indicator (SMA, EMA, RSI, MACD) |
| `period` | integer | Period used for calculation |
| `value` | number | Indicator value |
| `signal` | number | Signal line value (for MACD) |
| `histogram` | number | Histogram value (for MACD) |
| `metadata` | string | Additional metadata |

**Status Codes:**
- `200 OK`: Technical indicators calculated successfully
- `400 Bad Request`: Invalid request parameters
- `404 Not Found`: No stock data found for the symbol
- `500 Internal Server Error`: Server error

---

### **2.2 Get Calculated Technical Indicators**
**`GET /api/v1/analysis/technical/{symbol}`**

**Description:** Retrieves previously calculated technical indicators from the database.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol | `AAPL` |

**Query Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `indicatorType` | enum | Yes | Type of indicator | `SMA`, `EMA`, `RSI`, `MACD` |
| `startDate` | date | Yes | Start date | `2024-01-01` |
| `endDate` | date | Yes | End date | `2024-01-31` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/analysis/technical/AAPL?indicatorType=SMA&startDate=2024-01-01&endDate=2024-01-31" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
[
  {
    "symbol": "AAPL",
    "date": "2024-01-22",
    "indicatorType": "SMA",
    "period": 20,
    "value": 185.45,
    "signal": null,
    "histogram": null,
    "metadata": null
  }
]
```

**Status Codes:**
- `200 OK`: Technical indicators retrieved successfully
- `400 Bad Request`: Invalid parameters
- `404 Not Found`: No indicators found
- `500 Internal Server Error`: Server error

---

### **2.3 Get Trading Signals**
**`GET /api/v1/analysis/signals/{symbol}`**

**Description:** Generates trading signals (BUY/SELL/HOLD) based on technical analysis of the specified stock over the given time period.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol | `AAPL` |

**Query Parameters:**
| Parameter | Type | Required | Default | Description | Example |
|-----------|------|----------|---------|-------------|---------|
| `days` | integer | No | 50 | Number of days to analyze (minimum 50) | `100` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/analysis/signals/AAPL?days=100" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "SMA": "BUY",
  "RSI": "HOLD",
  "MACD": "BUY",
  "OVERALL": "BUY"
}
```

**Signal Values:**
| Value | Description |
|-------|-------------|
| `BUY` | Bullish signal - suggests buying |
| `SELL` | Bearish signal - suggests selling |
| `HOLD` | Neutral signal - suggests holding |

**Status Codes:**
- `200 OK`: Trading signals generated successfully
- `400 Bad Request`: Invalid symbol or days parameter
- `404 Not Found`: No data found for the symbol
- `500 Internal Server Error`: Server error

---

### **2.4 Get Available Technical Indicators**
**`GET /api/v1/analysis/indicators`**

**Description:** Returns a list of all available technical indicators that can be calculated.

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/analysis/indicators" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "availableIndicators": [
    {
      "name": "SMA_20",
      "description": "Simple Moving Average (20 periods)"
    },
    {
      "name": "SMA_50",
      "description": "Simple Moving Average (50 periods)"
    },
    {
      "name": "RSI_14",
      "description": "Relative Strength Index (14 periods)"
    }
  ],
  "signalTypes": ["BUY", "SELL", "HOLD"],
  "supportedPeriods": {
    "minimum": 20,
    "maximum": 365,
    "recommended": 50
  }
}
```

**Status Codes:**
- `200 OK`: Available indicators retrieved successfully

---

## **3. LLM Analysis APIs**

### **3.1 Get AI Stock Analysis**
**`POST /api/v1/analysis/llm`**

**Description:** Uses local LLM to analyze stock data and provide insights, trends, and trading recommendations.

**Request Payload:**
```json
{
  "symbol": "AAPL",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "analysisType": "stock-analysis",
  "includeTechnicalIndicators": true,
  "customPrompt": "Focus on risk assessment and long-term outlook"
}
```

**Request Fields:**
| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol | `AAPL` |
| `startDate` | date | No | Start date (defaults to 30 days ago) | `2024-01-01` |
| `endDate` | date | No | End date (defaults to today) | `2024-01-31` |
| `analysisType` | string | No | Type of analysis (default: "stock-analysis") | `stock-analysis` |
| `includeTechnicalIndicators` | boolean | No | Include technical indicators (default: true) | `true` |
| `customPrompt` | string | No | Custom analysis prompt | `"Focus on risk assessment"` |

**Available Analysis Types:**
| Type | Description |
|------|-------------|
| `stock-analysis` | Comprehensive stock analysis with trends and recommendations |
| `sentiment-analysis` | Market sentiment and mood analysis |
| `technical-analysis` | Technical indicator interpretation and signals |
| `custom` | Custom analysis with user-defined prompts |

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/analysis/llm" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "symbol": "AAPL",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "analysisType": "stock-analysis",
    "includeTechnicalIndicators": true
  }'
```

**Response Payload:**
```json
{
  "response": "Based on the analysis of AAPL stock data from January 2024, I observe the following:\n\n**Price Trends:**\n- The stock showed a strong upward trend with consistent higher highs and higher lows\n- Average daily volume increased by 15% compared to previous periods\n\n**Technical Signals:**\n- SMA crossover indicates bullish momentum\n- RSI at 65 suggests the stock is approaching overbought territory\n- MACD shows positive divergence\n\n**Recommendation:** BUY\n**Reasoning:** Strong technical indicators combined with increasing volume suggest continued upward momentum. However, monitor RSI levels for potential overbought conditions.\n\n**Key Risks:**\n- Market volatility could impact short-term performance\n- RSI approaching overbought levels may indicate near-term correction\n\n**Opportunities:**\n- Strong technical setup suggests potential for continued gains\n- Volume increase indicates institutional interest",
  "model": "mistral:7b",
  "timestamp": "2024-09-05T19:30:00",
  "processingTimeMs": 2500,
  "analysisType": "stock-analysis",
  "symbol": "AAPL"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `response` | string | AI-generated analysis text |
| `model` | string | LLM model used for analysis |
| `timestamp` | datetime | Analysis timestamp |
| `processingTimeMs` | integer | Processing time in milliseconds |
| `analysisType` | string | Type of analysis performed |
| `symbol` | string | Stock symbol analyzed |

**Status Codes:**
- `200 OK`: AI analysis completed successfully
- `400 Bad Request`: Invalid request parameters
- `404 Not Found`: No stock data found for the symbol
- `500 Internal Server Error`: Server error

---

### **3.2 Get Quick AI Analysis**
**`GET /api/v1/analysis/llm/{symbol}/quick`**

**Description:** Provides a quick AI analysis of recent stock data with basic insights and recommendations.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `symbol` | string | Yes | Stock symbol | `AAPL` |

**Query Parameters:**
| Parameter | Type | Required | Default | Description | Example |
|-----------|------|----------|---------|-------------|---------|
| `days` | integer | No | 30 | Number of days to analyze (1-90) | `7` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/analysis/llm/AAPL/quick?days=7" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
"AAPL Analysis (Last 7 Days):\n\nPrice Action: Stock closed at $151.75, up 2.1% from week start. Strong momentum with consistent gains.\n\nVolume: Above average volume indicates strong institutional interest.\n\nRecommendation: BUY - Strong technical setup with bullish momentum.\n\nRisk Level: Medium - Monitor for any reversal signals."
```

**Status Codes:**
- `200 OK`: Quick analysis completed successfully
- `400 Bad Request`: Invalid symbol or days parameter
- `404 Not Found`: No data found for the symbol
- `500 Internal Server Error`: Server error

---

### **3.3 Check LLM Service Status**
**`GET /api/v1/analysis/llm/status`**

**Description:** Tests the connection to the local LLM service and returns its status.

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/analysis/llm/status" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "status": "UP",
  "connected": true,
  "availableModels": ["mistral:7b", "llama2:7b"],
  "modelCount": 2,
  "timestamp": "2024-09-05T19:30:00"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `status` | string | Service status (UP/DOWN) |
| `connected` | boolean | Connection status |
| `availableModels` | array | List of available models |
| `modelCount` | integer | Number of available models |
| `timestamp` | datetime | Status check timestamp |

**Status Codes:**
- `200 OK`: LLM status retrieved successfully
- `503 Service Unavailable`: LLM service unavailable

---

### **3.4 Get Available Analysis Types**
**`GET /api/v1/analysis/llm/types`**

**Description:** Returns a list of all available AI analysis types.

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/analysis/llm/types" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "analysisTypes": [
    {
      "type": "stock-analysis",
      "description": "Comprehensive stock analysis with trends and recommendations"
    },
    {
      "type": "sentiment-analysis",
      "description": "Market sentiment and mood analysis"
    },
    {
      "type": "technical-analysis",
      "description": "Technical indicator interpretation and signals"
    },
    {
      "type": "custom",
      "description": "Custom analysis with user-defined prompts"
    }
  ],
  "supportedFeatures": [
    "Price trend analysis",
    "Volume analysis",
    "Technical indicator interpretation",
    "Trading recommendations",
    "Risk assessment",
    "Market sentiment analysis"
  ]
}
```

**Status Codes:**
- `200 OK`: Analysis types retrieved successfully

---

## **4. Health & Status APIs**

### **4.1 Application Health Check**
**`GET /api/v1/health`**

**Description:** Returns the overall health status of the application including database, cache, and LLM service status.

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/health" \
  -H "Accept: application/json"
```

### **4.2 Rate Limit Status**
**`GET /api/v1/rate-limit/status`**

**Description:** Returns the current rate limit status for all API providers (Alpha Vantage, EODHD).

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/rate-limit/status" \
  -H "Accept: application/json"
```

**Example Response:**
```json
{
  "timestamp": "2025-09-05T23:03:36.215211",
  "alpha-vantage": "API: alpha-vantage, Minute: 0/5, Day: 0/25",
  "eodhd": "API: eodhd, Minute: 0/60, Day: 0/20"
}
```

### **4.3 Rate Limit Status for Provider**
**`GET /api/v1/rate-limit/status/{provider}`**

**Description:** Returns the rate limit status for a specific API provider.

**Path Parameters:**
| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `provider` | string | Yes | API provider name | `alpha-vantage`, `eodhd` |

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/rate-limit/status/alpha-vantage" \
  -H "Accept: application/json"
```

**Example Response:**
```json
{
  "provider": "alpha-vantage",
  "status": "API: alpha-vantage, Minute: 0/5, Day: 0/25",
  "canMakeCall": true,
  "timestamp": "2025-09-05T23:03:36.215211"
}
```

**Response Payload:**
```json
{
  "status": "UP",
  "timestamp": "2024-09-05T19:30:00",
  "version": "1.0.0",
  "services": {
    "llm": {
      "status": "UP",
      "available": true
    },
    "cache": {
      "status": "UP",
      "totalKeys": 45,
      "stockDataKeys": 20,
      "technicalAnalysisKeys": 15,
      "llmAnalysisKeys": 10
    },
    "database": {
      "status": "UP",
      "type": "PostgreSQL/H2"
    }
  }
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `status` | string | Overall application status (UP/DOWN) |
| `timestamp` | datetime | Health check timestamp |
| `version` | string | Application version |
| `services` | object | Individual service statuses |

**Status Codes:**
- `200 OK`: Application is healthy
- `503 Service Unavailable`: Application has issues

---

### **4.2 LLM Service Status**
**`GET /api/v1/status/llm`**

**Description:** Returns detailed status information about the local LLM service.

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/status/llm" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "status": "UP",
  "connected": true,
  "availableModels": ["mistral:7b", "llama2:7b"],
  "modelCount": 2,
  "timestamp": "2024-09-05T19:30:00"
}
```

**Status Codes:**
- `200 OK`: LLM status retrieved successfully
- `503 Service Unavailable`: LLM service unavailable

---

### **4.3 Cache Statistics**
**`GET /api/v1/status/cache`**

**Description:** Returns detailed statistics about the Redis cache including key counts, TTL information, and performance metrics.

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/status/cache" \
  -H "Accept: application/json"
```

**Response Payload:**
```json
{
  "status": "UP",
  "statistics": {
    "totalKeys": 45,
    "stockDataKeys": 20,
    "technicalAnalysisKeys": 15,
    "llmAnalysisKeys": 10
  },
  "timestamp": "2024-09-05T19:30:00"
}
```

**Response Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `status` | string | Cache service status |
| `statistics` | object | Cache statistics |
| `statistics.totalKeys` | integer | Total number of cached keys |
| `statistics.stockDataKeys` | integer | Number of stock data keys |
| `statistics.technicalAnalysisKeys` | integer | Number of technical analysis keys |
| `statistics.llmAnalysisKeys` | integer | Number of LLM analysis keys |
| `timestamp` | datetime | Statistics timestamp |

**Status Codes:**
- `200 OK`: Cache statistics retrieved successfully

---

## **Error Responses**

### **400 Bad Request**
```json
{
  "timestamp": "2024-09-05T19:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "validationErrors": {
    "symbol": "Symbol must be 1-5 uppercase letters",
    "days": "Must be between 1 and 365"
  },
  "path": "/api/v1/stocks/INVALID/range"
}
```

### **404 Not Found**
```json
{
  "timestamp": "2024-09-05T19:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "No data found for the symbol",
  "path": "/api/v1/stocks/INVALID/latest"
}
```

### **500 Internal Server Error**
```json
{
  "timestamp": "2024-09-05T19:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/v1/stocks/AAPL/range"
}
```

---

## **Rate Limits**

| API Category | Rate Limit | Description |
|--------------|------------|-------------|
| **Stock Data APIs** | 5 requests/minute | Alpha Vantage free tier limit |
| **Technical Analysis APIs** | No limits | Calculated locally |
| **LLM Analysis APIs** | No limits | Local LLM processing |
| **Health APIs** | No limits | System monitoring |

---

## **Caching**

| Cache Type | Default TTL | Configurable | Description |
|------------|-------------|--------------|-------------|
| **Stock Data** | 1 hour | Yes | Stock price data caching |
| **Technical Analysis** | 30 minutes | Yes | Calculated indicators caching |
| **LLM Analysis** | 2 hours | Yes | AI analysis results caching |

**Cache Configuration:**
```yaml
app:
  cache:
    stock-data-ttl: 3600      # 1 hour in seconds
    technical-analysis-ttl: 1800  # 30 minutes in seconds
    llm-analysis-ttl: 7200    # 2 hours in seconds
```

---

## **Getting Started**

### **1. Start the Application**
```bash
cd backend
./mvnw spring-boot:run
```

### **2. Access Swagger UI**
Open your browser and navigate to:
```
http://localhost:8080/swagger-ui/index.html
```

### **3. Test Health Endpoint**
```bash
curl http://localhost:8080/api/v1/health
```

### **4. Get Stock Data**
```bash
curl "http://localhost:8080/api/v1/stocks/AAPL?days=7"
```

### **5. Calculate Technical Indicators**
```bash
curl -X POST "http://localhost:8080/api/v1/analysis/technical" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "indicators": ["SMA_20", "RSI_14"]
  }'
```

### **6. Get AI Analysis**
```bash
curl -X POST "http://localhost:8080/api/v1/analysis/llm" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "analysisType": "stock-analysis"
  }'
```

---

## **Best Practices**

1. **Use appropriate date ranges** - Don't request too much historical data at once
2. **Cache results** - The system automatically caches responses for optimal performance
3. **Monitor rate limits** - Respect the 5 requests/minute limit for stock data APIs
4. **Handle errors gracefully** - Always check response status codes
5. **Use Swagger UI** - Interactive documentation for testing and exploration
6. **Check service health** - Monitor the health endpoints for system status

---

## **Support**

For issues, questions, or contributions:
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **Health Check**: `http://localhost:8080/api/v1/health`
- **OpenAPI Spec**: `http://localhost:8080/api-docs`

---

*This documentation is automatically generated and kept up-to-date with the API implementation.*
