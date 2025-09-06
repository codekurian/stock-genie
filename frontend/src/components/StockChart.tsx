import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  AreaChart,
  Area,
} from 'recharts';
import { StockData } from '../services/api';

interface StockChartProps {
  data: StockData[];
  symbol: string;
}

const StockChart: React.FC<StockChartProps> = ({ data, symbol }) => {
  if (!data || data.length === 0) {
    return (
      <div className="flex items-center justify-center h-96 text-gray-500">
        <div className="text-center">
          <p className="text-lg font-medium">No data available</p>
          <p className="text-sm">Please select a stock symbol to view the chart</p>
        </div>
      </div>
    );
  }

  // Transform data for the chart
  const chartData = data.map((item) => ({
    date: new Date(item.date).toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric' 
    }),
    fullDate: item.date,
    open: item.open,
    high: item.high,
    low: item.low,
    close: item.close,
    volume: item.volume,
  }));

  const formatTooltipValue = (value: number, name: string) => {
    const formattedValue = value.toFixed(2);
    switch (name) {
      case 'close':
        return [`$${formattedValue}`, 'Close Price'];
      case 'open':
        return [`$${formattedValue}`, 'Open Price'];
      case 'high':
        return [`$${formattedValue}`, 'High Price'];
      case 'low':
        return [`$${formattedValue}`, 'Low Price'];
      case 'volume':
        return [value.toLocaleString(), 'Volume'];
      default:
        return [formattedValue, name];
    }
  };

  const latestPrice = data[data.length - 1]?.close || 0;
  const firstPrice = data[0]?.close || 0;
  const priceChange = latestPrice - firstPrice;
  const priceChangePercent = firstPrice > 0 ? (priceChange / firstPrice) * 100 : 0;
  const isPositive = priceChange >= 0;

  return (
    <div className="space-y-6">
      {/* Chart Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">{symbol}</h2>
          <div className="flex items-center space-x-4 mt-2">
            <span className="text-3xl font-bold text-gray-900">
              ${latestPrice.toFixed(2)}
            </span>
            <span className={`text-lg font-medium ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
              {isPositive ? '+' : ''}${priceChange.toFixed(2)} ({isPositive ? '+' : ''}{priceChangePercent.toFixed(2)}%)
            </span>
          </div>
        </div>
        <div className="text-right text-sm text-gray-500">
          <p>Last updated: {new Date().toLocaleString()}</p>
          <p>Data source: {data[0]?.dataSource || 'Unknown'}</p>
        </div>
      </div>

      {/* Price Chart */}
      <div className="h-96">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <defs>
              <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={isPositive ? "#10b981" : "#ef4444"} stopOpacity={0.3}/>
                <stop offset="95%" stopColor={isPositive ? "#10b981" : "#ef4444"} stopOpacity={0}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis 
              dataKey="date" 
              stroke="#6b7280"
              fontSize={12}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              stroke="#6b7280"
              fontSize={12}
              tickLine={false}
              axisLine={false}
              domain={['dataMin - 5', 'dataMax + 5']}
              tickFormatter={(value) => `$${value.toFixed(0)}`}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'white',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
              }}
              formatter={formatTooltipValue}
              labelFormatter={(label) => `Date: ${label}`}
            />
            <Area
              type="monotone"
              dataKey="close"
              stroke={isPositive ? "#10b981" : "#ef4444"}
              strokeWidth={2}
              fillOpacity={1}
              fill="url(#colorPrice)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      {/* Volume Chart */}
      <div className="h-32">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">Volume</h3>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
            <XAxis 
              dataKey="date" 
              stroke="#6b7280"
              fontSize={10}
              tickLine={false}
              axisLine={false}
            />
            <YAxis 
              stroke="#6b7280"
              fontSize={10}
              tickLine={false}
              axisLine={false}
              tickFormatter={(value) => `${(value / 1000000).toFixed(1)}M`}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: 'white',
                border: '1px solid #e5e7eb',
                borderRadius: '8px',
                boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
              }}
              formatter={formatTooltipValue}
            />
            <Line
              type="monotone"
              dataKey="volume"
              stroke="#3b82f6"
              strokeWidth={1}
              dot={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Data Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t border-gray-200">
        <div className="text-center">
          <p className="text-sm text-gray-500">Period</p>
          <p className="font-semibold text-gray-900">{data.length} days</p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Avg Volume</p>
          <p className="font-semibold text-gray-900">
            {Math.round(data.reduce((sum, item) => sum + item.volume, 0) / data.length).toLocaleString()}
          </p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Highest</p>
          <p className="font-semibold text-gray-900">
            ${Math.max(...data.map(item => item.high)).toFixed(2)}
          </p>
        </div>
        <div className="text-center">
          <p className="text-sm text-gray-500">Lowest</p>
          <p className="font-semibold text-gray-900">
            ${Math.min(...data.map(item => item.low)).toFixed(2)}
          </p>
        </div>
      </div>
    </div>
  );
};

export default StockChart;
