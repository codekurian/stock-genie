# 🧠 LLM Setup Guide for Stock Genie

## Overview

This guide covers the setup and configuration of Local Language Models (LLMs) for the Stock Genie application. We use Ollama to run models locally, ensuring privacy and eliminating API costs.

## 🚀 Quick Setup

### Option 1: Quick Setup (Recommended for getting started)
```bash
./scripts/quick-llm-setup.sh
```

### Option 2: Full Setup (Complete configuration)
```bash
./scripts/setup-llm.sh
```

## 📋 Prerequisites

- **macOS**: Homebrew installed
- **Linux**: Standard package manager
- **RAM**: Minimum 8GB (16GB recommended)
- **Storage**: 10GB free space for models

## 🔧 Manual Installation

### macOS
```bash
# Install Ollama via Homebrew
brew install ollama

# Start Ollama service
brew services start ollama

# Pull recommended models
ollama pull mistral:7b
ollama pull llama2:7b
```

### Linux
```bash
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama service
systemctl --user start ollama

# Pull recommended models
ollama pull mistral:7b
ollama pull llama2:7b
```

## 🧪 Testing the Setup

Run the test script to verify everything is working:
```bash
./scripts/test-llm.sh
```

## 📊 Available Models

### Primary Models

| Model | Size | Best For | RAM Required |
|-------|------|----------|--------------|
| **Mistral 7B** | 4.4GB | General stock analysis | 8GB |
| **Llama 2 7B** | 3.8GB | Sentiment analysis | 8GB |

### Advanced Models (Optional)

| Model | Size | Best For | RAM Required |
|-------|------|----------|--------------|
| **Mistral 7B Instruct** | 4.4GB | Detailed analysis | 8GB |
| **Llama 2 13B** | 7.3GB | Complex reasoning | 16GB |
| **Code Llama 7B** | 3.8GB | Data processing | 8GB |

## ⚙️ Configuration

The LLM configuration is stored in `config/llm-config.yml`:

```yaml
llm:
  provider: ollama
  base-url: http://localhost:11434
  models:
    primary: mistral:7b
    backup: llama2:7b
  settings:
    timeout: 30000
    max-tokens: 1000
    temperature: 0.7
    top-p: 0.9
```

## 🎯 Usage Examples

### Basic Stock Analysis
```bash
ollama run mistral:7b "Analyze this stock data: AAPL, \$150, \$155, \$148, \$152, 1000000 volume. Provide a brief technical analysis."
```

### Technical Indicators
```bash
ollama run mistral:7b "Explain RSI and MACD indicators briefly."
```

### Market Sentiment
```bash
ollama run mistral:7b "What factors influence stock market sentiment?"
```

## 🔧 Troubleshooting

### Common Issues

#### 1. Ollama Not Starting
```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Restart Ollama
brew services restart ollama  # macOS
systemctl --user restart ollama  # Linux
```

#### 2. Model Not Found
```bash
# List available models
ollama list

# Pull missing model
ollama pull mistral:7b
```

#### 3. Out of Memory
- Close other applications
- Use smaller models (7B instead of 13B)
- Increase swap space

#### 4. Slow Performance
- Ensure sufficient RAM (8GB+)
- Close unnecessary applications
- Use SSD storage

## 📈 Performance Optimization

### Memory Optimization
```bash
# Set environment variables for better performance
export OLLAMA_FLASH_ATTENTION=1
export OLLAMA_KV_CACHE_TYPE=q8_0
```

### Model Selection
- **For speed**: Use Mistral 7B
- **For accuracy**: Use Llama 2 13B
- **For balance**: Use Mistral 7B Instruct

## 🔄 Integration with Spring Boot

The LLM service will be integrated into the Spring Boot backend:

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
}
```

## 📚 Advanced Usage

### Custom Prompts
Create custom prompts for specific analysis types:

```yaml
prompts:
  risk-assessment: |
    Assess the risk level for {SYMBOL} based on:
    - Volatility: {VOLATILITY}
    - Beta: {BETA}
    - Financial metrics: {METRICS}
    
    Provide risk score (1-10) and reasoning.
```

### Batch Processing
Process multiple stocks simultaneously:

```bash
# Create batch script
echo "AAPL,150,155,148,152,1000000" | ollama run mistral:7b "Analyze this stock data..."
echo "GOOGL,2800,2850,2780,2820,500000" | ollama run mistral:7b "Analyze this stock data..."
```

## 🎯 Next Steps

1. ✅ **LLM Setup Complete** - Models are ready
2. 🔄 **Backend Integration** - Integrate with Spring Boot
3. 🔄 **API Development** - Create REST endpoints
4. 🔄 **Frontend Integration** - Connect to React dashboard

## 📞 Support

If you encounter issues:

1. Check the troubleshooting section above
2. Run the test script: `./scripts/test-llm.sh`
3. Check Ollama logs: `brew services list` (macOS) or `journalctl --user -u ollama` (Linux)
4. Verify system requirements (RAM, storage)

## 🔗 Useful Links

- [Ollama Documentation](https://ollama.ai/docs)
- [Mistral Models](https://huggingface.co/mistralai)
- [Llama 2 Models](https://huggingface.co/meta-llama)
- [Stock Genie Project](README.md)
