#!/bin/bash

# Alpha Vantage API Setup Script
# This script helps you get a free Alpha Vantage API key

echo "🚀 Alpha Vantage API Setup"
echo "=========================="
echo ""

echo "📋 Steps to get your free Alpha Vantage API key:"
echo ""
echo "1. 🌐 Visit: https://www.alphavantage.co/support/#api-key"
echo "2. 📝 Fill out the registration form with:"
echo "   - First Name"
echo "   - Last Name" 
echo "   - Email Address"
echo "   - Company (optional)"
echo "3. ✅ Check the terms and conditions"
echo "4. 🎯 Click 'GET FREE API KEY'"
echo "5. 📧 Check your email for the API key"
echo ""

echo "🔑 Once you have your API key, set it as an environment variable:"
echo ""
echo "   export ALPHA_VANTAGE_API_KEY=your_api_key_here"
echo ""
echo "   Or add it to your ~/.bashrc or ~/.zshrc:"
echo "   echo 'export ALPHA_VANTAGE_API_KEY=your_api_key_here' >> ~/.bashrc"
echo ""

echo "📊 Alpha Vantage Free Tier Limits:"
echo "   - 5 API calls per minute"
echo "   - 25 API calls per day"
echo "   - Perfect for development and testing"
echo ""

echo "💡 Pro Tips:"
echo "   - The API key is free and doesn't expire"
echo "   - Rate limits reset every minute/day"
echo "   - Use mock data when rate limits are exceeded"
echo "   - Consider upgrading for production use"
echo ""

echo "🧪 Test your API key:"
echo "   curl \"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=AAPL&apikey=YOUR_API_KEY\""
echo ""

echo "✅ Setup complete! Your Stock Genie app will automatically use the API key when available."
