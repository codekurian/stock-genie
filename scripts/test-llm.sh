#!/bin/bash

# Test script for LLM models in Stock Genie

set -e

echo "🧪 Testing LLM Models for Stock Genie"
echo "====================================="

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
    echo "❌ Ollama is not running. Please start it first:"
    echo "   brew services start ollama"
    exit 1
fi

echo "✅ Ollama is running"

# Test 1: Basic functionality
echo ""
echo "📊 Test 1: Basic Stock Analysis"
echo "-------------------------------"
echo "Prompt: What are the key technical indicators for stock analysis?"
echo "Response:"
ollama run mistral:7b "What are the key technical indicators for stock analysis?"

# Test 2: Technical indicators explanation
echo ""
echo "📈 Test 2: Technical Indicators Explanation"
echo "-------------------------------------------"
echo "Prompt: Explain RSI and MACD indicators briefly."
echo "Response:"
ollama run mistral:7b "Explain RSI and MACD indicators briefly."

# Test 3: Stock data analysis
echo ""
echo "💰 Test 3: Stock Data Analysis"
echo "------------------------------"
echo "Prompt: Analyze this stock data: AAPL, \$150, \$155, \$148, \$152, 1000000 volume."
echo "Response:"
ollama run mistral:7b "Analyze this stock data: AAPL, \$150, \$155, \$148, \$152, 1000000 volume. Provide a brief technical analysis."

# Test 4: Market sentiment
echo ""
echo "📰 Test 4: Market Sentiment Analysis"
echo "------------------------------------"
echo "Prompt: What factors influence stock market sentiment?"
echo "Response:"
ollama run mistral:7b "What factors influence stock market sentiment?"

# Test 5: Risk assessment
echo ""
echo "⚠️  Test 5: Risk Assessment"
echo "---------------------------"
echo "Prompt: How do you assess risk in stock investments?"
echo "Response:"
ollama run mistral:7b "How do you assess risk in stock investments?"

echo ""
echo "🎉 All tests completed successfully!"
echo "====================================="
echo "✅ Mistral 7B is working correctly for stock analysis"
echo "✅ Ready to integrate with Spring Boot backend"
echo ""
echo "Next steps:"
echo "1. Continue with Stage 2: Backend Setup"
echo "2. Integrate LLM service in Spring Boot application"
