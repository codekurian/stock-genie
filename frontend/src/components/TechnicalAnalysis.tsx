import React, { useState, useEffect } from 'react';
import { Activity, TrendingUp, TrendingDown, BarChart3, RefreshCw } from 'lucide-react';
import { analysisApi } from '../services/api';

interface TechnicalAnalysisProps {
  symbol: string;
}

interface IndicatorData {
  [key: string]: Array<{
    date: string;
    value: number;
  }>;
}

const TechnicalAnalysis: React.FC<TechnicalAnalysisProps> = ({ symbol }) => {
  const [indicators, setIndicators] = useState<string[]>([]);
  const [analysisData, setAnalysisData] = useState<IndicatorData>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedIndicators, setSelectedIndicators] = useState<string[]>([]);
  const [days, setDays] = useState(30);

  const fetchAvailableIndicators = async () => {
    try {
      const response = await analysisApi.getAvailableIndicators(symbol);
      setIndicators(response.availableIndicators || []);
    } catch (err) {
      setError('Failed to fetch available indicators');
      console.error('Error fetching indicators:', err);
    }
  };

  const fetchTechnicalAnalysis = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await analysisApi.getTechnicalAnalysis(
        symbol, 
        days, 
        selectedIndicators.length > 0 ? selectedIndicators : undefined
      );
      setAnalysisData(response.analysis || {});
    } catch (err) {
      setError('Failed to fetch technical analysis');
      console.error('Error fetching technical analysis:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (symbol) {
      fetchAvailableIndicators();
    }
  }, [symbol]);

  useEffect(() => {
    if (symbol && indicators.length > 0) {
      fetchTechnicalAnalysis();
    }
  }, [symbol, days, selectedIndicators]);

  const handleIndicatorToggle = (indicator: string) => {
    setSelectedIndicators(prev => 
      prev.includes(indicator) 
        ? prev.filter(i => i !== indicator)
        : [...prev, indicator]
    );
  };

  const getIndicatorDescription = (indicator: string) => {
    const descriptions: { [key: string]: string } = {
      'SMA_20': 'Simple Moving Average (20 periods) - Short-term trend',
      'SMA_50': 'Simple Moving Average (50 periods) - Medium-term trend',
      'EMA_12': 'Exponential Moving Average (12 periods) - Fast trend',
      'EMA_26': 'Exponential Moving Average (26 periods) - Slow trend',
      'RSI_14': 'Relative Strength Index (14 periods) - Momentum oscillator',
      'MACD': 'Moving Average Convergence Divergence - Trend changes',
      'OBV': 'On-Balance Volume - Volume-price relationship'
    };
    return descriptions[indicator] || 'Technical indicator';
  };

  const getIndicatorColor = (indicator: string) => {
    const colors: { [key: string]: string } = {
      'SMA_20': '#3b82f6',
      'SMA_50': '#1d4ed8',
      'EMA_12': '#10b981',
      'EMA_26': '#059669',
      'RSI_14': '#f59e0b',
      'MACD': '#8b5cf6',
      'OBV': '#ef4444'
    };
    return colors[indicator] || '#6b7280';
  };

  const getLatestValue = (indicator: string) => {
    const data = analysisData[indicator];
    if (!data || data.length === 0) return null;
    return data[data.length - 1]?.value;
  };

  const getIndicatorStatus = (indicator: string) => {
    const value = getLatestValue(indicator);
    if (value === null) return 'No Data';
    
    // Simple status logic based on indicator type
    if (indicator.includes('RSI')) {
      if (value > 70) return 'Overbought';
      if (value < 30) return 'Oversold';
      return 'Neutral';
    }
    
    if (indicator.includes('MACD')) {
      return value > 0 ? 'Bullish' : 'Bearish';
    }
    
    return 'Active';
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Overbought':
      case 'Bearish':
        return 'text-red-600 bg-red-100';
      case 'Oversold':
      case 'Bullish':
        return 'text-green-600 bg-green-100';
      case 'No Data':
        return 'text-gray-600 bg-gray-100';
      default:
        return 'text-blue-600 bg-blue-100';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Technical Analysis</h2>
          <p className="text-gray-600">Analyze price trends and momentum indicators</p>
        </div>
        <button
          onClick={fetchTechnicalAnalysis}
          disabled={loading}
          className="btn-secondary flex items-center space-x-2"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Controls */}
      <div className="flex flex-wrap items-center gap-4 p-4 bg-gray-50 rounded-lg">
        <div className="flex items-center space-x-2">
          <label className="text-sm font-medium text-gray-700">Period:</label>
          <select
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            className="input-field w-20"
          >
            <option value={7}>7 days</option>
            <option value={14}>14 days</option>
            <option value={30}>30 days</option>
            <option value={60}>60 days</option>
          </select>
        </div>
        
        <div className="flex items-center space-x-2">
          <label className="text-sm font-medium text-gray-700">Indicators:</label>
          <div className="flex flex-wrap gap-2">
            {indicators.map((indicator) => (
              <button
                key={indicator}
                onClick={() => handleIndicatorToggle(indicator)}
                className={`px-3 py-1 text-xs font-medium rounded-full transition-colors ${
                  selectedIndicators.includes(indicator)
                    ? 'bg-primary-600 text-white'
                    : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
                }`}
              >
                {indicator}
              </button>
            ))}
          </div>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-red-800">{error}</p>
        </div>
      )}

      {/* Indicators Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {indicators.map((indicator) => {
          const value = getLatestValue(indicator);
          const status = getIndicatorStatus(indicator);
          const color = getIndicatorColor(indicator);
          
          return (
            <div key={indicator} className="card">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center space-x-2">
                  <div 
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: color }}
                  />
                  <h3 className="font-semibold text-gray-900">{indicator}</h3>
                </div>
                <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(status)}`}>
                  {status}
                </span>
              </div>
              
              <div className="space-y-2">
                <div>
                  <p className="text-sm text-gray-500">Current Value</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {value !== null ? value.toFixed(4) : 'N/A'}
                  </p>
                </div>
                
                <div>
                  <p className="text-sm text-gray-500">Description</p>
                  <p className="text-sm text-gray-700">
                    {getIndicatorDescription(indicator)}
                  </p>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Analysis Summary */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Analysis Summary</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h4 className="font-medium text-gray-900 mb-2">Trend Analysis</h4>
            <div className="space-y-2">
              {indicators.filter(i => i.includes('SMA') || i.includes('EMA')).map(indicator => {
                const value = getLatestValue(indicator);
                const status = getIndicatorStatus(indicator);
                return (
                  <div key={indicator} className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">{indicator}</span>
                    <span className={`text-sm font-medium ${getStatusColor(status)}`}>
                      {status}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
          
          <div>
            <h4 className="font-medium text-gray-900 mb-2">Momentum Analysis</h4>
            <div className="space-y-2">
              {indicators.filter(i => i.includes('RSI') || i.includes('MACD')).map(indicator => {
                const value = getLatestValue(indicator);
                const status = getIndicatorStatus(indicator);
                return (
                  <div key={indicator} className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">{indicator}</span>
                    <span className={`text-sm font-medium ${getStatusColor(status)}`}>
                      {status}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Data Status */}
      <div className="text-center text-sm text-gray-500">
        <p>
          {Object.keys(analysisData).length === 0 
            ? 'No technical analysis data available. This may be due to insufficient historical data.'
            : `Loaded ${Object.keys(analysisData).length} indicators for ${symbol}`
          }
        </p>
      </div>
    </div>
  );
};

export default TechnicalAnalysis;
