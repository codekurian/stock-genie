import React, { useState, useEffect } from 'react';
import { TrendingUp, TrendingDown, Activity, Brain, BarChart3, RefreshCw } from 'lucide-react';
import { stockApi, analysisApi, llmApi, healthApi, StockData, LLMResponse } from '../services/api';
import StockChart from './StockChart';
import TechnicalAnalysis from './TechnicalAnalysis';
import LLMAnalysis from './LLMAnalysis';
import HealthStatus from './HealthStatus';

const Dashboard: React.FC = () => {
  const [selectedSymbol, setSelectedSymbol] = useState('AAPL');
  const [stockData, setStockData] = useState<StockData[]>([]);
  const [latestData, setLatestData] = useState<StockData | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'chart' | 'analysis' | 'llm'>('chart');

  const popularSymbols = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX'];

  const fetchStockData = async (symbol: string) => {
    setLoading(true);
    setError(null);
    try {
      const [data, latest] = await Promise.all([
        stockApi.getStockData(symbol, 30),
        stockApi.getLatestStockData(symbol)
      ]);
      setStockData(data);
      setLatestData(latest);
    } catch (err) {
      setError('Failed to fetch stock data');
      console.error('Error fetching stock data:', err);
    } finally {
      setLoading(false);
    }
  };

  const refreshData = async () => {
    if (selectedSymbol) {
      try {
        await stockApi.refreshStockData(selectedSymbol);
        await fetchStockData(selectedSymbol);
      } catch (err) {
        setError('Failed to refresh data');
        console.error('Error refreshing data:', err);
      }
    }
  };

  useEffect(() => {
    fetchStockData(selectedSymbol);
  }, [selectedSymbol]);

  const getPriceChange = () => {
    if (!stockData || stockData.length < 2) return { change: 0, percentage: 0 };
    
    const latest = stockData[stockData.length - 1];
    const previous = stockData[stockData.length - 2];
    const change = latest.close - previous.close;
    const percentage = (change / previous.close) * 100;
    
    return { change, percentage };
  };

  const priceChange = getPriceChange();
  const isPositive = priceChange.change >= 0;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-3">
              <div className="bg-primary-600 p-2 rounded-lg">
                <TrendingUp className="h-6 w-6 text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">Stock Genie</h1>
                <p className="text-sm text-gray-500">AI-Powered Stock Analysis</p>
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              <HealthStatus />
              <button
                onClick={refreshData}
                disabled={loading}
                className="btn-secondary flex items-center space-x-2"
              >
                <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                <span>Refresh</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Symbol Selector */}
        <div className="mb-8">
          <div className="flex flex-wrap gap-2 mb-4">
            {popularSymbols.map((symbol) => (
              <button
                key={symbol}
                onClick={() => setSelectedSymbol(symbol)}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                  selectedSymbol === symbol
                    ? 'bg-primary-600 text-white'
                    : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-300'
                }`}
              >
                {symbol}
              </button>
            ))}
          </div>
          
          <div className="flex items-center space-x-4">
            <input
              type="text"
              value={selectedSymbol}
              onChange={(e) => setSelectedSymbol(e.target.value.toUpperCase())}
              placeholder="Enter stock symbol"
              className="input-field max-w-xs"
            />
            <button
              onClick={() => fetchStockData(selectedSymbol)}
              disabled={loading || !selectedSymbol}
              className="btn-primary"
            >
              {loading ? 'Loading...' : 'Load Data'}
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-red-800">{error}</p>
          </div>
        )}

        {/* Stock Overview */}
        {latestData && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
            <div className="card">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500">Current Price</p>
                  <p className="text-2xl font-bold text-gray-900">
                    ${latestData.close.toFixed(2)}
                  </p>
                </div>
                <div className={`p-2 rounded-lg ${isPositive ? 'bg-green-100' : 'bg-red-100'}`}>
                  {isPositive ? (
                    <TrendingUp className="h-6 w-6 text-green-600" />
                  ) : (
                    <TrendingDown className="h-6 w-6 text-red-600" />
                  )}
                </div>
              </div>
              <div className="mt-2">
                <span className={`text-sm font-medium ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                  {isPositive ? '+' : ''}${priceChange.change.toFixed(2)} ({isPositive ? '+' : ''}{priceChange.percentage.toFixed(2)}%)
                </span>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500">Volume</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {latestData.volume.toLocaleString()}
                  </p>
                </div>
                <Activity className="h-6 w-6 text-gray-400" />
              </div>
            </div>

            <div className="card">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500">High</p>
                  <p className="text-2xl font-bold text-gray-900">
                    ${latestData.high.toFixed(2)}
                  </p>
                </div>
                <TrendingUp className="h-6 w-6 text-green-500" />
              </div>
            </div>

            <div className="card">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-500">Low</p>
                  <p className="text-2xl font-bold text-gray-900">
                    ${latestData.low.toFixed(2)}
                  </p>
                </div>
                <TrendingDown className="h-6 w-6 text-red-500" />
              </div>
            </div>
          </div>
        )}

        {/* Navigation Tabs */}
        <div className="mb-6">
          <nav className="flex space-x-8">
            <button
              onClick={() => setActiveTab('chart')}
              className={`flex items-center space-x-2 py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'chart'
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <BarChart3 className="h-4 w-4" />
              <span>Price Chart</span>
            </button>
            <button
              onClick={() => setActiveTab('analysis')}
              className={`flex items-center space-x-2 py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'analysis'
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <Activity className="h-4 w-4" />
              <span>Technical Analysis</span>
            </button>
            <button
              onClick={() => setActiveTab('llm')}
              className={`flex items-center space-x-2 py-2 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'llm'
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <Brain className="h-4 w-4" />
              <span>AI Analysis</span>
            </button>
          </nav>
        </div>

        {/* Tab Content */}
        <div className="card">
          {activeTab === 'chart' && (
            <StockChart data={stockData} symbol={selectedSymbol} />
          )}
          {activeTab === 'analysis' && (
            <TechnicalAnalysis symbol={selectedSymbol} />
          )}
          {activeTab === 'llm' && (
            <LLMAnalysis symbol={selectedSymbol} stockData={stockData} />
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
