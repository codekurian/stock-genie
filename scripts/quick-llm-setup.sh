#!/bin/bash

# Quick LLM Setup Script for Stock Genie
# Minimal setup for getting started quickly

set -e

echo "🚀 Quick LLM Setup for Stock Genie"
echo "=================================="

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "❌ Ollama not found. Installing..."
    
    # Detect OS and install
    if [[ "$OSTYPE" == "darwin"* ]]; then
        if command -v brew &> /dev/null; then
            brew install ollama
        else
            echo "❌ Homebrew not found. Please install Homebrew first."
            exit 1
        fi
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        curl -fsSL https://ollama.ai/install.sh | sh
    else
        echo "❌ Unsupported OS. Please install Ollama manually."
        exit 1
    fi
else
    echo "✅ Ollama already installed"
fi

# Start Ollama service
echo "🔄 Starting Ollama service..."
if [[ "$OSTYPE" == "darwin"* ]]; then
    brew services start ollama 2>/dev/null || ollama serve &
else
    systemctl --user start ollama 2>/dev/null || ollama serve &
fi

# Wait for Ollama to be ready
echo "⏳ Waiting for Ollama to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
        echo "✅ Ollama is ready!"
        break
    fi
    sleep 2
    echo -n "."
done

# Pull Mistral 7B (primary model)
echo "📥 Installing Mistral 7B (recommended for stock analysis)..."
ollama pull mistral:7b

# Test the model
echo "🧪 Testing Mistral 7B..."
echo "Test prompt: Analyze this stock data: AAPL, \$150, \$155, \$148, \$152, 1000000 volume."
echo "Response:"
ollama run mistral:7b "Analyze this stock data: AAPL, \$150, \$155, \$148, \$152, 1000000 volume. Provide a brief technical analysis."

echo ""
echo "🎉 LLM Setup Complete!"
echo "======================"
echo "✅ Ollama running on http://localhost:11434"
echo "✅ Mistral 7B ready for stock analysis"
echo ""
echo "Next steps:"
echo "1. Continue with Stage 2: Backend Setup"
echo "2. Or run: ./scripts/setup-llm.sh for full setup with additional models"
