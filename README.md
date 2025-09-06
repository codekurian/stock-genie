# 🚀 Stock Genie - Local Development Project Plan

## 📋 Local Development Setup Overview

### **Recommended Local LLM Options for Stock Analysis:**

1. **Ollama (Recommended)**
   - **Models**: Llama 2 7B/13B, Mistral 7B, Code Llama
   - **Pros**: Easy setup, good performance, active community
   - **Cons**: Requires 8GB+ RAM for 7B models

2. **GPT4All**
   - **Models**: Various optimized models
   - **Pros**: Lightweight, good for lower-end hardware
   - **Cons**: Limited model selection

3. **LocalAI**
   - **Models**: Compatible with OpenAI API format
   - **Pros**: Drop-in replacement for OpenAI API
   - **Cons**: More complex setup

**Recommendation**: Start with **Ollama + Mistral 7B** for best balance of performance and resource usage.

---

## 🏗️ Stage-by-Stage Implementation Plan

### **Prerequisites Setup**
```bash
# Required software
- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Ollama (for local LLM)
- Git
```

---

## **Stage 1: Local Environment Setup** (Day 1)

### 1.1 Install Ollama and Setup Local LLM
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Pull recommended model for financial analysis
ollama pull mistral:7b
ollama pull llama2:7b

# Test the model
ollama run mistral:7b "Analyze this stock data: AAPL, $150, $155, $148, $152, 1000000 volume"
```

### 1.2 Create Project Structure in Cursor
```bash
mkdir stock-genie
cd stock-genie
mkdir -p {backend,frontend,docker,scripts,docs}
```

### 1.3 Docker Compose Setup
Create `docker-compose.yml` for local development:
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: stockgenie
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    environment:
      - OLLAMA_HOST=0.0.0.0

volumes:
  postgres_data:
  ollama_data:
```

---

## **Stage 2: Backend Foundation** (Day 2-3)

### 2.1 Spring Boot Project Setup
```bash
# In Cursor terminal
cd backend
curl https://start.spring.io/starter.zip \
  -d dependencies=web,data-jpa,postgresql,redis,validation,actuator \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.2.0 \
  -d baseDir=stock-genie-backend \
  -d groupId=com.stockgenie \
  -d artifactId=stock-genie-backend \
  -d name=stock-genie-backend \
  -d description="Stock Analysis Backend" \
  -d packageName=com.stockgenie \
  -d packaging=jar \
  -d javaVersion=17 \
  -o stock-genie-backend.zip

unzip stock-genie-backend.zip
```

### 2.2 Core Dependencies (pom.xml additions)
```xml
<dependencies>
    <!-- Technical Analysis -->
    <dependency>
        <groupId>org.ta4j</groupId>
        <artifactId>ta4j-core</artifactId>
        <version>0.18</version>
    </dependency>
    
    <!-- HTTP Client -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Local LLM Integration -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 2.3 Application Configuration
Create `application-local.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/stockgenie
    username: postgres
    password: password
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

# Local LLM Configuration
local-llm:
  provider: ollama
  base-url: http://localhost:11434
  model: mistral:7b
  timeout: 30000

# Financial API (use free tier)
financial:
  api:
    provider: alpha-vantage
    api-key: ${ALPHA_VANTAGE_API_KEY:demo}
    base-url: https://www.alphavantage.co/query
```

---

## **Stage 3: Data Layer Implementation** (Day 3-4)

### 3.1 Database Entities
Create entities for:
- `StockData` (OHLCV data)
- `TechnicalAnalysis` (calculated indicators)
- `AnalysisRequest` (LLM analysis requests)

### 3.2 Repository Layer
Implement JPA repositories with custom queries for:
- Stock data retrieval by date range
- Technical indicator calculations
- Analysis request tracking

### 3.3 Local LLM Service
```java
@Service
public class LocalLLMService {
    
    @Value("${local-llm.base-url}")
    private String baseUrl;
    
    @Value("${local-llm.model}")
    private String model;
    
    public String analyzeStockData(String symbol, List<StockData> data, 
                                 Map<String, Object> technicalIndicators) {
        String prompt = buildAnalysisPrompt(symbol, data, technicalIndicators);
        return callOllamaAPI(prompt);
    }
    
    private String callOllamaAPI(String prompt) {
        // Implementation to call Ollama API
        // POST to http://localhost:11434/api/generate
    }
}
```

---

## **Stage 4: Financial Data Integration** (Day 4-5)

### 4.1 Alpha Vantage Integration
```java
@Service
public class FinancialDataService {
    
    public List<StockData> fetchStockData(String symbol, LocalDate startDate, LocalDate endDate) {
        // Check database first
        if (hasDataInRange(symbol, startDate, endDate)) {
            return getDataFromDatabase(symbol, startDate, endDate);
        }
        
        // Fetch from API
        return fetchFromAlphaVantage(symbol, startDate, endDate);
    }
}
```

### 4.2 Data Caching Strategy
- Redis for API response caching
- Database for persistent storage
- Smart cache invalidation

---

## **Stage 5: Technical Analysis Engine** (Day 5-6)

### 5.1 TA4J Integration
```java
@Service
public class TechnicalAnalysisService {
    
    public Map<String, Object> calculateIndicators(List<StockData> stockData) {
        TimeSeries series = convertToTimeSeries(stockData);
        
        Map<String, Object> indicators = new HashMap<>();
        
        // Moving Averages
        indicators.put("sma_20", new SMAIndicator(closePrice, 20));
        indicators.put("ema_12", new EMAIndicator(closePrice, 12));
        
        // RSI
        indicators.put("rsi_14", new RSIIndicator(closePrice, 14));
        
        // MACD
        indicators.put("macd", new MACDIndicator(closePrice, 12, 26, 9));
        
        return indicators;
    }
}
```

### 5.2 Signal Generation
Implement buy/sell/hold signals based on technical indicators.

---

## **Stage 6: API Endpoints** (Day 6-7)

### 6.1 REST Controllers
```java
@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {
    
    @GetMapping("/{symbol}/data")
    public ResponseEntity<List<StockDataDto>> getStockData(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // Implementation
    }
    
    @PostMapping("/{symbol}/analyze")
    public ResponseEntity<AnalysisResponse> analyzeStock(
            @PathVariable String symbol,
            @RequestBody AnalysisRequest request) {
        // Implementation with local LLM
    }
}
```

---

## **Stage 7: Frontend Dashboard** (Day 7-10)

### 7.1 React Setup
```bash
cd frontend
npx create-react-app stock-genie-frontend --template typescript
cd stock-genie-frontend
npm install @mui/material @emotion/react @emotion/styled
npm install recharts react-query axios
```

### 7.2 Key Components
- `StockChart` - Price visualization
- `TechnicalIndicators` - Indicator display
- `AIAnalysis` - LLM insights
- `PortfolioView` - Portfolio management

### 7.3 Chart Integration
```typescript
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

const StockChart: React.FC<{ data: StockData[] }> = ({ data }) => {
  return (
    <ResponsiveContainer width="100%" height={400}>
      <LineChart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" />
        <YAxis />
        <Tooltip />
        <Line type="monotone" dataKey="close" stroke="#8884d8" />
      </LineChart>
    </ResponsiveContainer>
  );
};
```

---

## **Stage 8: Local LLM Integration** (Day 8-9)

### 8.1 Ollama API Integration
```typescript
// Frontend service
export const analyzeStockWithLLM = async (symbol: string, data: any[]) => {
  const response = await fetch('/api/v1/stocks/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ symbol, data })
  });
  return response.json();
};
```

### 8.2 Prompt Engineering
```java
private String buildAnalysisPrompt(String symbol, List<StockData> data, 
                                 Map<String, Object> indicators) {
    return String.format("""
        Analyze the following historical stock data for %s:
        
        Data: %s
        
        Technical Indicators: %s
        
        Provide analysis on:
        1. Price trends
        2. Technical signals
        3. Risk assessment
        4. Buy/Sell/Hold recommendation
        
        Keep response concise and actionable.
        """, symbol, formatStockData(data), formatIndicators(indicators));
}
```

---

## **Stage 9: Testing & Optimization** (Day 9-10)

### 9.1 Local Testing
```bash
# Start all services
docker-compose up -d

# Start backend
cd backend && ./mvnw spring-boot:run

# Start frontend
cd frontend && npm start

# Test API
curl http://localhost:8080/api/v1/stocks/AAPL/data?startDate=2024-01-01&endDate=2024-01-31
```

### 9.2 Performance Optimization
- Database query optimization
- Caching strategies
- LLM response caching
- Frontend bundle optimization

---

## **Stage 10: Local Deployment** (Day 10)

### 10.1 Docker Compose for Full Stack
```yaml
version: '3.8'
services:
  # Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: stockgenie
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # Cache
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  # Local LLM
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    environment:
      - OLLAMA_HOST=0.0.0.0

  # Backend
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - redis
      - ollama
    environment:
      - SPRING_PROFILES_ACTIVE=local

  # Frontend
  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend

volumes:
  postgres_data:
  ollama_data:
```

---

## 🚀 Execution Commands for Cursor

### **Daily Development Workflow:**

```bash
# Morning startup
docker-compose up -d
cd backend && ./mvnw spring-boot:run &
cd frontend && npm start &

# Check services
curl http://localhost:8080/actuator/health
curl http://localhost:11434/api/tags

# Test LLM
ollama run mistral:7b "What is the current market trend for technology stocks?"
```

### **Stage Execution Commands:**

```bash
# Stage 1: Setup
./scripts/setup-local-env.sh

# Stage 2: Backend
./scripts/setup-backend.sh

# Stage 3: Database
./scripts/setup-database.sh

# Stage 4: API Integration
./scripts/test-api-integration.sh

# Stage 5: Technical Analysis
./scripts/test-technical-analysis.sh

# Stage 6: LLM Integration
./scripts/test-llm-integration.sh

# Stage 7: Frontend
./scripts/setup-frontend.sh

# Stage 8: Full Stack Test
./scripts/test-full-stack.sh
```

---

## 📊 Local Development Benefits

1. **Privacy**: All data stays local
2. **Cost**: No API costs for LLM usage
3. **Speed**: No network latency for LLM calls
4. **Customization**: Full control over models and prompts
5. **Offline**: Works without internet (except for stock data)

---

## 🛠️ Technology Stack Summary

### Backend
- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL (production), H2 (development)
- **Cache**: Redis
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Testing**: JUnit 5, TestContainers, Mockito

### Frontend
- **Framework**: React 18 with TypeScript
- **UI Library**: Material-UI or Ant Design
- **State Management**: Redux Toolkit
- **Charts**: TradingView Charting Library or Chart.js
- **Build Tool**: Vite

### Local LLM
- **Platform**: Ollama
- **Models**: Mistral 7B, Llama 2 7B
- **Integration**: REST API calls to local Ollama instance

### DevOps
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Monitoring**: Spring Boot Actuator
- **Logging**: SLF4J + Logback

---

## 📊 Key Features Timeline

| Day | Stage | Key Deliverables |
|-----|-------|------------------|
| 1   | Local Environment | Ollama setup, Docker containers |
| 2-3 | Backend Foundation | Spring Boot, database setup |
| 3-4 | Data Layer | Entities, repositories, LLM service |
| 4-5 | Financial Data | Alpha Vantage integration, caching |
| 5-6 | Technical Analysis | TA4J indicators, signal generation |
| 6-7 | API Endpoints | REST controllers, documentation |
| 7-10| Frontend Dashboard | React app, charts, UI components |
| 8-9 | LLM Integration | Ollama API, prompt engineering |
| 9-10| Testing & Optimization | Performance tuning, testing |
| 10  | Local Deployment | Full stack deployment |

---

## 🎯 Success Metrics

1. **Performance**: API response time < 200ms
2. **Reliability**: 99.9% uptime locally
3. **Accuracy**: Technical indicators match industry standards
4. **User Experience**: Dashboard load time < 3 seconds
5. **LLM Performance**: Analysis generation < 10 seconds

---

## 🔧 Troubleshooting

### Common Issues:

1. **Ollama not responding**
   ```bash
   # Check if Ollama is running
   curl http://localhost:11434/api/tags
   
   # Restart Ollama
   docker-compose restart ollama
   ```

2. **Database connection issues**
   ```bash
   # Check PostgreSQL status
   docker-compose logs postgres
   
   # Reset database
   docker-compose down -v && docker-compose up -d
   ```

3. **Frontend build issues**
   ```bash
   # Clear node modules and reinstall
   rm -rf node_modules package-lock.json
   npm install
   ```

---

## 📚 Additional Resources

- [Ollama Documentation](https://ollama.ai/docs)
- [Spring Boot Reference](https://spring.io/projects/spring-boot)
- [TA4J Documentation](https://ta4j.github.io/ta4j-wiki/)
- [Alpha Vantage API](https://www.alphavantage.co/documentation/)
- [React Documentation](https://react.dev/)

---

This plan ensures you can build and test everything locally while maintaining professional-grade architecture and features.
