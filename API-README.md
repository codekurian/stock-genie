# 📚 Stock Genie API Documentation

## **Quick Start Guide**

### **🚀 Start the Application**
```bash
cd backend
./mvnw spring-boot:run
```

### **📖 Access Documentation**
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI Spec**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/api/v1/health

### **🧪 Test the APIs**
```bash
# Run comprehensive API tests
./scripts/test-apis.sh

# Or test individual endpoints
curl http://localhost:8080/api/v1/health
curl "http://localhost:8080/api/v1/stocks/AAPL?days=7"
```

---

## **📋 API Overview**

| Category | Endpoints | Description |
|----------|-----------|-------------|
| **Stock Data** | 4 endpoints | Fetch historical and real-time stock data |
| **Technical Analysis** | 4 endpoints | Calculate indicators and trading signals |
| **AI Analysis** | 4 endpoints | Get LLM-powered insights and recommendations |
| **Health & Status** | 3 endpoints | Monitor application and service health |

---

## **🔗 API Endpoints**

### **Stock Data APIs**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/stocks/{symbol}` | Get recent stock data |
| `GET` | `/api/v1/stocks/{symbol}/latest` | Get latest stock data |
| `GET` | `/api/v1/stocks/{symbol}/range` | Get data for date range |
| `GET` | `/api/v1/stocks/{symbol}/info` | Get data availability info |

### **Technical Analysis APIs**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/analysis/indicators` | Get available indicators |
| `POST` | `/api/v1/analysis/technical` | Calculate technical indicators |
| `GET` | `/api/v1/analysis/technical/{symbol}` | Get calculated indicators |
| `GET` | `/api/v1/analysis/signals/{symbol}` | Get trading signals |

### **AI Analysis APIs**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/analysis/llm/status` | Check LLM service status |
| `GET` | `/api/v1/analysis/llm/types` | Get available analysis types |
| `GET` | `/api/v1/analysis/llm/{symbol}/quick` | Get quick AI analysis |
| `POST` | `/api/v1/analysis/llm` | Get full AI analysis |

### **Health & Status APIs**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/health` | Application health check |
| `GET` | `/api/v1/status/llm` | LLM service status |
| `GET` | `/api/v1/status/cache` | Cache statistics |

---

## **📊 Example Usage**

### **1. Get Stock Data**
```bash
# Get recent data
curl "http://localhost:8080/api/v1/stocks/AAPL?days=7"

# Get data for specific range
curl "http://localhost:8080/api/v1/stocks/AAPL/range?startDate=2024-01-01&endDate=2024-01-31"
```

### **2. Calculate Technical Indicators**
```bash
curl -X POST "http://localhost:8080/api/v1/analysis/technical" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "indicators": ["SMA_20", "RSI_14", "MACD"]
  }'
```

### **3. Get AI Analysis**
```bash
curl -X POST "http://localhost:8080/api/v1/analysis/llm" \
  -H "Content-Type: application/json" \
  -d '{
    "symbol": "AAPL",
    "analysisType": "stock-analysis"
  }'
```

### **4. Get Trading Signals**
```bash
curl "http://localhost:8080/api/v1/analysis/signals/AAPL?days=50"
```

---

## **⚙️ Configuration**

### **Caching Configuration**
```yaml
app:
  cache:
    stock-data-ttl: 3600      # 1 hour
    technical-analysis-ttl: 1800  # 30 minutes
    llm-analysis-ttl: 7200    # 2 hours
```

### **Rate Limits**
- **Stock Data APIs**: 5 requests/minute
- **Technical Analysis**: No limits
- **LLM Analysis**: No limits

---

## **🔧 Available Technical Indicators**

| Indicator | Description | Period |
|-----------|-------------|---------|
| `SMA_20` | Simple Moving Average | 20 periods |
| `SMA_50` | Simple Moving Average | 50 periods |
| `EMA_12` | Exponential Moving Average | 12 periods |
| `EMA_26` | Exponential Moving Average | 26 periods |
| `RSI_14` | Relative Strength Index | 14 periods |
| `MACD` | Moving Average Convergence Divergence | 12,26,9 |
| `OBV` | On-Balance Volume | N/A |

---

## **🤖 AI Analysis Types**

| Type | Description |
|------|-------------|
| `stock-analysis` | Comprehensive stock analysis with trends and recommendations |
| `sentiment-analysis` | Market sentiment and mood analysis |
| `technical-analysis` | Technical indicator interpretation and signals |
| `custom` | Custom analysis with user-defined prompts |

---

## **📈 Response Examples**

### **Stock Data Response**
```json
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
}
```

### **Technical Analysis Response**
```json
{
  "SMA_20": [
    {
      "symbol": "AAPL",
      "date": "2024-01-22",
      "indicatorType": "SMA",
      "period": 20,
      "value": 185.45
    }
  ]
}
```

### **AI Analysis Response**
```json
{
  "response": "Based on the analysis of AAPL stock data...",
  "model": "mistral:7b",
  "timestamp": "2024-09-05T19:30:00",
  "processingTimeMs": 2500,
  "analysisType": "stock-analysis",
  "symbol": "AAPL"
}
```

### **Trading Signals Response**
```json
{
  "SMA": "BUY",
  "RSI": "HOLD",
  "MACD": "BUY",
  "OVERALL": "BUY"
}
```

---

## **❌ Error Handling**

### **Common Error Responses**

**400 Bad Request**
```json
{
  "timestamp": "2024-09-05T19:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "path": "/api/v1/stocks/INVALID/range"
}
```

**404 Not Found**
```json
{
  "timestamp": "2024-09-05T19:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "No data found for the symbol",
  "path": "/api/v1/stocks/INVALID/latest"
}
```

**500 Internal Server Error**
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

## **🧪 Testing**

### **Automated Testing**
```bash
# Run comprehensive API tests
./scripts/test-apis.sh
```

### **Manual Testing**
1. **Start the application**: `./mvnw spring-boot:run`
2. **Open Swagger UI**: http://localhost:8080/swagger-ui/index.html
3. **Test endpoints** using the interactive interface
4. **Check health**: http://localhost:8080/api/v1/health

### **Test Coverage**
- ✅ All 15 API endpoints tested
- ✅ Error handling scenarios
- ✅ Request validation
- ✅ Response format validation
- ✅ Service health checks

---

## **📚 Additional Resources**

- **Full API Documentation**: [API-DOCUMENTATION.md](./API-DOCUMENTATION.md)
- **Project README**: [README.md](./README.md)
- **LLM Setup Guide**: [docs/LLM-SETUP.md](./docs/LLM-SETUP.md)
- **Docker Compose Guide**: [docs/DOCKER-COMPOSE-EXPLAINED.md](./docs/DOCKER-COMPOSE-EXPLAINED.md)

---

## **🆘 Troubleshooting**

### **Common Issues**

1. **Application won't start**
   - Check Java version: `java -version` (requires Java 17+)
   - Check port availability: `lsof -i :8080`

2. **APIs returning errors**
   - Check application logs
   - Verify all services are running (Redis, LLM)
   - Test health endpoint: `curl http://localhost:8080/api/v1/health`

3. **LLM analysis failing**
   - Check LLM status: `curl http://localhost:8080/api/v1/analysis/llm/status`
   - Ensure Ollama is running: `ollama list`
   - Verify models are downloaded: `ollama pull mistral:7b`

4. **No stock data returned**
   - Check Alpha Vantage API key configuration
   - Verify symbol is valid (e.g., AAPL, MSFT, GOOGL)
   - Check date ranges are valid

### **Support**
- **Health Check**: http://localhost:8080/api/v1/health
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Application Logs**: Check console output for detailed error messages

---

*This API documentation is automatically generated and kept up-to-date with the implementation.*
