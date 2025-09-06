import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Types
export interface StockData {
  symbol: string;
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  adjustedClose: number;
  dataSource: string;
}

export interface TechnicalAnalysis {
  symbol: string;
  indicator: string;
  date: string;
  value: number;
  parameters: string;
}

export interface LLMRequest {
  symbol: string;
  analysisType: 'quick' | 'comprehensive';
  days: number;
  includeTechnicalAnalysis?: boolean;
  customPrompt?: string;
}

export interface LLMResponse {
  symbol: string;
  analysis: string;
  recommendation: 'BUY' | 'SELL' | 'HOLD';
  confidence: number;
  timestamp: string;
  technicalSummary?: string;
}

export interface HealthStatus {
  status: string;
  version: string;
  message: string;
  timestamp: string;
}

export interface LLMStatus {
  available: boolean;
  model: string;
  endpoint: string;
  message: string;
}

// API Functions
export const stockApi = {
  // Get stock data for a symbol
  getStockData: async (symbol: string, days: number = 30): Promise<StockData[]> => {
    const response = await api.get(`/stocks/${symbol}?days=${days}`);
    return response.data;
  },

  // Get latest stock data
  getLatestStockData: async (symbol: string): Promise<StockData> => {
    const response = await api.get(`/stocks/${symbol}/latest`);
    return response.data;
  },

  // Refresh stock data
  refreshStockData: async (symbol: string): Promise<string> => {
    const response = await api.post(`/stocks/${symbol}/refresh`);
    return response.data;
  },
};

export const analysisApi = {
  // Get available indicators
  getAvailableIndicators: async (symbol: string) => {
    const response = await api.get(`/analysis/${symbol}/indicators`);
    return response.data;
  },

  // Get technical analysis
  getTechnicalAnalysis: async (symbol: string, days: number = 30, indicators?: string[]) => {
    const params = new URLSearchParams();
    params.append('days', days.toString());
    if (indicators && indicators.length > 0) {
      params.append('indicators', indicators.join(','));
    }
    const response = await api.get(`/analysis/${symbol}/technical?${params}`);
    return response.data;
  },

  // Calculate specific indicators
  calculateIndicators: async (symbol: string, days: number, indicators: string[]) => {
    const response = await api.post(`/analysis/${symbol}/calculate`, {
      days,
      indicators,
    });
    return response.data;
  },
};

export const llmApi = {
  // Get LLM status
  getStatus: async (): Promise<LLMStatus> => {
    const response = await api.get('/llm/status');
    return response.data;
  },

  // Analyze stock with LLM
  analyzeStock: async (request: LLMRequest): Promise<LLMResponse> => {
    const response = await api.post('/llm/analyze', request);
    return response.data;
  },

  // Test LLM
  testLLM: async (prompt: string): Promise<string> => {
    const response = await api.post('/llm/test', { prompt });
    return response.data;
  },
};

export const healthApi = {
  // Get health status
  getHealth: async (): Promise<HealthStatus> => {
    const response = await api.get('/health');
    return response.data;
  },
};

export default api;
