#!/bin/bash

# Stock Genie - LLM Setup Script
# This script sets up Ollama and downloads recommended models for stock analysis

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to detect OS
detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "linux"
    else
        echo "unknown"
    fi
}

# Function to install Ollama on macOS
install_ollama_macos() {
    print_status "Installing Ollama on macOS..."
    
    if command_exists brew; then
        print_status "Using Homebrew to install Ollama..."
        brew install ollama
    else
        print_error "Homebrew not found. Please install Homebrew first:"
        echo "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
        exit 1
    fi
}

# Function to install Ollama on Linux
install_ollama_linux() {
    print_status "Installing Ollama on Linux..."
    curl -fsSL https://ollama.ai/install.sh | sh
}

# Function to install Ollama
install_ollama() {
    local os=$(detect_os)
    
    if command_exists ollama; then
        print_success "Ollama is already installed"
        return 0
    fi
    
    print_status "Installing Ollama..."
    
    case $os in
        "macos")
            install_ollama_macos
            ;;
        "linux")
            install_ollama_linux
            ;;
        *)
            print_error "Unsupported operating system: $OSTYPE"
            print_error "Please install Ollama manually from https://ollama.ai"
            exit 1
            ;;
    esac
}

# Function to start Ollama service
start_ollama() {
    print_status "Starting Ollama service..."
    
    local os=$(detect_os)
    
    case $os in
        "macos")
            if command_exists brew; then
                brew services start ollama
            else
                print_warning "Starting Ollama manually..."
                ollama serve &
                sleep 5
            fi
            ;;
        "linux")
            print_status "Starting Ollama service..."
            systemctl --user start ollama 2>/dev/null || {
                print_warning "Could not start as systemd service, starting manually..."
                ollama serve &
                sleep 5
            }
            ;;
    esac
    
    # Wait for Ollama to be ready
    print_status "Waiting for Ollama to be ready..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:11434/api/tags >/dev/null 2>&1; then
            print_success "Ollama is ready!"
            return 0
        fi
        
        print_status "Attempt $attempt/$max_attempts - Waiting for Ollama..."
        sleep 2
        ((attempt++))
    done
    
    print_error "Ollama failed to start after $max_attempts attempts"
    exit 1
}

# Function to pull a model
pull_model() {
    local model=$1
    local description=$2
    
    print_status "Pulling $description ($model)..."
    
    if ollama list | grep -q "$model"; then
        print_success "$description is already installed"
        return 0
    fi
    
    if ollama pull "$model"; then
        print_success "Successfully pulled $description"
    else
        print_error "Failed to pull $description"
        return 1
    fi
}

# Function to test a model
test_model() {
    local model=$1
    local description=$2
    
    print_status "Testing $description..."
    
    local test_prompt="Analyze this stock data: AAPL, \$150, \$155, \$148, \$152, 1000000 volume. Provide a brief technical analysis."
    
    echo -e "${YELLOW}Test prompt:${NC} $test_prompt"
    echo -e "${YELLOW}Model response:${NC}"
    
    if echo "$test_prompt" | ollama run "$model" --verbose; then
        print_success "$description is working correctly"
    else
        print_error "Failed to test $description"
        return 1
    fi
}

# Function to show available models
show_models() {
    print_status "Available models:"
    ollama list
}

# Function to show model info
show_model_info() {
    local model=$1
    print_status "Model information for $model:"
    ollama show "$model"
}

# Function to create model configuration
create_model_config() {
    print_status "Creating model configuration..."
    
    cat > config/llm-config.yml << EOF
# Stock Genie - LLM Configuration
llm:
  provider: ollama
  base-url: http://localhost:11434
  models:
    primary: mistral:7b
    backup: llama2:7b
    specialized:
      - mistral:7b  # Best for general stock analysis
      - llama2:7b   # Good for sentiment analysis
  settings:
    timeout: 30000
    max-tokens: 1000
    temperature: 0.7
    top-p: 0.9
  prompts:
    stock-analysis: |
      You are a financial analyst. Analyze the following stock data for {SYMBOL}:
      
      Price Data: {PRICE_DATA}
      Technical Indicators: {INDICATORS}
      
      Provide:
      1. Trend analysis
      2. Technical signal interpretation
      3. Risk assessment
      4. Buy/Sell/Hold recommendation with reasoning
      
      Keep response concise and actionable.
    
    sentiment-analysis: |
      As a specialized financial analyst, evaluate this stock data:
      
      {STOCK_DATA}
      
      Focus on:
      - Sentiment indicators
      - Market psychology
      - Risk factors
      - Investment recommendation
EOF

    print_success "Model configuration created at config/llm-config.yml"
}

# Function to create test script
create_test_script() {
    print_status "Creating test script..."
    
    cat > scripts/test-llm.sh << 'EOF'
#!/bin/bash

# Test script for LLM models

echo "Testing LLM models..."

echo "1. Testing Mistral 7B:"
ollama run mistral:7b "What are the key technical indicators for stock analysis?"

echo -e "\n2. Testing Llama 2 7B:"
ollama run llama2:7b "Explain RSI and MACD indicators briefly."

echo -e "\n3. Testing stock analysis:"
ollama run mistral:7b "Analyze this stock data: AAPL, \$150, \$155, \$148, \$152, 1000000 volume. Provide a brief technical analysis."

echo -e "\nAll tests completed!"
EOF

    chmod +x scripts/test-llm.sh
    print_success "Test script created at scripts/test-llm.sh"
}

# Main function
main() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                    Stock Genie - LLM Setup                  ║"
    echo "║                                                              ║"
    echo "║  This script will install and configure Ollama with         ║"
    echo "║  recommended models for stock analysis.                     ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # Create necessary directories
    mkdir -p scripts config
    
    # Install Ollama
    install_ollama
    
    # Start Ollama service
    start_ollama
    
    # Pull recommended models
    print_status "Installing recommended models for stock analysis..."
    
    pull_model "mistral:7b" "Mistral 7B (Primary - Best for general stock analysis)"
    pull_model "llama2:7b" "Llama 2 7B (Backup - Good for sentiment analysis)"
    
    # Test models
    print_status "Testing installed models..."
    
    test_model "mistral:7b" "Mistral 7B"
    echo ""
    test_model "llama2:7b" "Llama 2 7B"
    
    # Show available models
    echo ""
    show_models
    
    # Create configuration files
    create_model_config
    create_test_script
    
    # Final success message
    echo ""
    echo -e "${GREEN}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                    Setup Complete!                          ║"
    echo "║                                                              ║"
    echo "║  ✅ Ollama installed and running                            ║"
    echo "║  ✅ Mistral 7B model ready                                  ║"
    echo "║  ✅ Llama 2 7B model ready                                  ║"
    echo "║  ✅ Configuration files created                             ║"
    echo "║  ✅ Test script created                                     ║"
    echo "║                                                              ║"
    echo "║  Next steps:                                                ║"
    echo "║  1. Run: ./scripts/test-llm.sh                             ║"
    echo "║  2. Check: config/llm-config.yml                           ║"
    echo "║  3. Continue with Stage 2: Backend Setup                    ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    print_success "LLM setup completed successfully!"
    print_status "Ollama is running on http://localhost:11434"
    print_status "You can now proceed with the backend development"
}

# Handle command line arguments
case "${1:-}" in
    "test")
        if [ -n "${2:-}" ]; then
            test_model "$2" "$2"
        else
            echo "Usage: $0 test <model-name>"
            echo "Available models:"
            ollama list
        fi
        ;;
    "list")
        show_models
        ;;
    "info")
        if [ -n "${2:-}" ]; then
            show_model_info "$2"
        else
            echo "Usage: $0 info <model-name>"
        fi
        ;;
    "start")
        start_ollama
        ;;
    *)
        main
        ;;
esac
