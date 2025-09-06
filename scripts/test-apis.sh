#!/bin/bash

# Stock Genie API Testing Script
# This script tests all the major API endpoints to ensure they're working correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="http://localhost:8080/api/v1"
SYMBOL="AAPL"
START_DATE="2024-01-01"
END_DATE="2024-01-31"

echo -e "${BLUE}🚀 Stock Genie API Testing Script${NC}"
echo -e "${BLUE}=================================${NC}"
echo ""

# Function to test API endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "${YELLOW}Testing: ${description}${NC}"
    echo -e "${BLUE}${method} ${endpoint}${NC}"
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "${BASE_URL}${endpoint}")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$data" "${BASE_URL}${endpoint}")
    fi
    
    # Extract status code (last line)
    status_code=$(echo "$response" | tail -n1)
    # Extract response body (all but last line)
    response_body=$(echo "$response" | head -n -1)
    
    if [ "$status_code" -ge 200 ] && [ "$status_code" -lt 300 ]; then
        echo -e "${GREEN}✅ SUCCESS (${status_code})${NC}"
        echo "$response_body" | jq . 2>/dev/null || echo "$response_body"
    else
        echo -e "${RED}❌ FAILED (${status_code})${NC}"
        echo "$response_body"
    fi
    echo ""
}

# Check if application is running
echo -e "${YELLOW}🔍 Checking if application is running...${NC}"
if ! curl -s "${BASE_URL}/health" > /dev/null; then
    echo -e "${RED}❌ Application is not running on ${BASE_URL}${NC}"
    echo -e "${YELLOW}Please start the application first:${NC}"
    echo "  cd backend && ./mvnw spring-boot:run"
    exit 1
fi
echo -e "${GREEN}✅ Application is running${NC}"
echo ""

# Test 1: Health Check
test_endpoint "GET" "/health" "" "Application Health Check"

# Test 2: Stock Data - Get Recent Data
test_endpoint "GET" "/stocks/${SYMBOL}?days=7" "" "Get Recent Stock Data (7 days)"

# Test 3: Stock Data - Get Latest Data
test_endpoint "GET" "/stocks/${SYMBOL}/latest" "" "Get Latest Stock Data"

# Test 4: Stock Data - Get Data for Date Range
test_endpoint "GET" "/stocks/${SYMBOL}/range?startDate=${START_DATE}&endDate=${END_DATE}" "" "Get Stock Data for Date Range"

# Test 5: Stock Data - Get Data Availability Info
test_endpoint "GET" "/stocks/${SYMBOL}/info" "" "Get Stock Data Availability Info"

# Test 6: Technical Analysis - Get Available Indicators
test_endpoint "GET" "/analysis/indicators" "" "Get Available Technical Indicators"

# Test 7: Technical Analysis - Calculate Indicators
test_endpoint "POST" "/analysis/technical" "{
  \"symbol\": \"${SYMBOL}\",
  \"startDate\": \"${START_DATE}\",
  \"endDate\": \"${END_DATE}\",
  \"indicators\": [\"SMA_20\", \"RSI_14\", \"MACD\"]
}" "Calculate Technical Indicators"

# Test 8: Technical Analysis - Get Trading Signals
test_endpoint "GET" "/analysis/signals/${SYMBOL}?days=50" "" "Get Trading Signals"

# Test 9: LLM Analysis - Check LLM Status
test_endpoint "GET" "/analysis/llm/status" "" "Check LLM Service Status"

# Test 10: LLM Analysis - Get Available Analysis Types
test_endpoint "GET" "/analysis/llm/types" "" "Get Available LLM Analysis Types"

# Test 11: LLM Analysis - Quick Analysis
test_endpoint "GET" "/analysis/llm/${SYMBOL}/quick?days=7" "" "Get Quick LLM Analysis"

# Test 12: LLM Analysis - Full Analysis
test_endpoint "POST" "/analysis/llm" "{
  \"symbol\": \"${SYMBOL}\",
  \"startDate\": \"${START_DATE}\",
  \"endDate\": \"${END_DATE}\",
  \"analysisType\": \"stock-analysis\",
  \"includeTechnicalIndicators\": true
}" "Get Full LLM Analysis"

# Test 13: Status - Cache Statistics
test_endpoint "GET" "/status/cache" "" "Get Cache Statistics"

# Test 14: Status - LLM Service Status
test_endpoint "GET" "/status/llm" "" "Get LLM Service Status"

# Test 15: Error Handling - Invalid Symbol
test_endpoint "GET" "/stocks/INVALID/latest" "" "Test Error Handling (Invalid Symbol)"

# Test 16: Error Handling - Invalid Date Range
test_endpoint "GET" "/stocks/${SYMBOL}/range?startDate=invalid&endDate=invalid" "" "Test Error Handling (Invalid Dates)"

echo -e "${GREEN}🎉 API Testing Complete!${NC}"
echo ""
echo -e "${BLUE}📊 Summary:${NC}"
echo "- All major API endpoints have been tested"
echo "- Check the results above for any failures"
echo "- For interactive testing, visit: http://localhost:8080/swagger-ui/index.html"
echo ""
echo -e "${YELLOW}💡 Tips:${NC}"
echo "- If any tests fail, check the application logs"
echo "- Ensure all required services (Redis, LLM) are running"
echo "- Some tests may fail if no data is available for the test symbol"
echo ""
echo -e "${BLUE}🔗 Useful Links:${NC}"
echo "- Swagger UI: http://localhost:8080/swagger-ui/index.html"
echo "- Health Check: http://localhost:8080/api/v1/health"
echo "- OpenAPI Spec: http://localhost:8080/api-docs"
