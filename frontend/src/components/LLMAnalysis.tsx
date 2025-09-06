import React, { useState, useEffect } from 'react';
import { Brain, Send, RefreshCw, AlertCircle, CheckCircle, Clock } from 'lucide-react';
import { llmApi, LLMRequest, LLMResponse, StockData } from '../services/api';

interface LLMAnalysisProps {
  symbol: string;
  stockData: StockData[];
}

const LLMAnalysis: React.FC<LLMAnalysisProps> = ({ symbol, stockData }) => {
  const [llmStatus, setLlmStatus] = useState<{ available: boolean; message: string } | null>(null);
  const [analysis, setAnalysis] = useState<LLMResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analysisType, setAnalysisType] = useState<'quick' | 'comprehensive'>('quick');
  const [days, setDays] = useState(7);
  const [customPrompt, setCustomPrompt] = useState('');
  const [includeTechnical, setIncludeTechnical] = useState(true);

  const fetchLLMStatus = async () => {
    try {
      const status = await llmApi.getStatus();
      setLlmStatus(status);
    } catch (err) {
      setLlmStatus({ available: false, message: 'Failed to check LLM status' });
      console.error('Error fetching LLM status:', err);
    }
  };

  const runAnalysis = async () => {
    if (!llmStatus?.available) {
      setError('LLM service is not available');
      return;
    }

    setLoading(true);
    setError(null);
    setAnalysis(null);

    try {
      const request: LLMRequest = {
        symbol,
        analysisType,
        days,
        includeTechnicalAnalysis: includeTechnical,
        customPrompt: customPrompt || undefined,
      };

      const response = await llmApi.analyzeStock(request);
      setAnalysis(response);
    } catch (err) {
      setError('Failed to get AI analysis');
      console.error('Error running analysis:', err);
    } finally {
      setLoading(false);
    }
  };

  const testLLM = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await llmApi.testLLM('Hello, can you analyze stock data?');
      setError(null);
      // You could show this in a modal or toast
      console.log('LLM Test Response:', response);
    } catch (err) {
      setError('LLM test failed');
      console.error('Error testing LLM:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLLMStatus();
  }, []);

  const getRecommendationColor = (recommendation: string) => {
    switch (recommendation) {
      case 'BUY':
        return 'text-green-600 bg-green-100';
      case 'SELL':
        return 'text-red-600 bg-red-100';
      case 'HOLD':
        return 'text-yellow-600 bg-yellow-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 80) return 'text-green-600';
    if (confidence >= 60) return 'text-yellow-600';
    return 'text-red-600';
  };

  const formatAnalysisText = (text: string) => {
    return text.split('\n').map((line, index) => (
      <p key={index} className="mb-2">
        {line}
      </p>
    ));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">AI Stock Analysis</h2>
          <p className="text-gray-600">Get AI-powered insights and recommendations</p>
        </div>
        <div className="flex items-center space-x-2">
          {llmStatus && (
            <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm ${
              llmStatus.available 
                ? 'bg-green-100 text-green-800' 
                : 'bg-red-100 text-red-800'
            }`}>
              {llmStatus.available ? (
                <CheckCircle className="h-4 w-4" />
              ) : (
                <AlertCircle className="h-4 w-4" />
              )}
              <span>{llmStatus.available ? 'LLM Ready' : 'LLM Offline'}</span>
            </div>
          )}
        </div>
      </div>

      {/* LLM Status */}
      {llmStatus && (
        <div className={`p-4 rounded-lg border ${
          llmStatus.available 
            ? 'bg-green-50 border-green-200' 
            : 'bg-red-50 border-red-200'
        }`}>
          <div className="flex items-center space-x-2">
            {llmStatus.available ? (
              <CheckCircle className="h-5 w-5 text-green-600" />
            ) : (
              <AlertCircle className="h-5 w-5 text-red-600" />
            )}
            <p className={`font-medium ${
              llmStatus.available ? 'text-green-800' : 'text-red-800'
            }`}>
              {llmStatus.message}
            </p>
          </div>
        </div>
      )}

      {/* Analysis Controls */}
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Analysis Configuration</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Analysis Type
              </label>
              <div className="flex space-x-4">
                <label className="flex items-center">
                  <input
                    type="radio"
                    value="quick"
                    checked={analysisType === 'quick'}
                    onChange={(e) => setAnalysisType(e.target.value as 'quick' | 'comprehensive')}
                    className="mr-2"
                  />
                  <span className="text-sm">Quick Analysis</span>
                </label>
                <label className="flex items-center">
                  <input
                    type="radio"
                    value="comprehensive"
                    checked={analysisType === 'comprehensive'}
                    onChange={(e) => setAnalysisType(e.target.value as 'quick' | 'comprehensive')}
                    className="mr-2"
                  />
                  <span className="text-sm">Comprehensive</span>
                </label>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Time Period
              </label>
              <select
                value={days}
                onChange={(e) => setDays(Number(e.target.value))}
                className="input-field"
              >
                <option value={7}>7 days</option>
                <option value={14}>14 days</option>
                <option value={30}>30 days</option>
                <option value={60}>60 days</option>
              </select>
            </div>

            <div>
              <label className="flex items-center">
                <input
                  type="checkbox"
                  checked={includeTechnical}
                  onChange={(e) => setIncludeTechnical(e.target.checked)}
                  className="mr-2"
                />
                <span className="text-sm text-gray-700">Include Technical Analysis</span>
              </label>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Custom Prompt (Optional)
              </label>
              <textarea
                value={customPrompt}
                onChange={(e) => setCustomPrompt(e.target.value)}
                placeholder="Add specific questions or focus areas for the analysis..."
                className="input-field h-24 resize-none"
              />
            </div>

            <div className="flex space-x-2">
              <button
                onClick={runAnalysis}
                disabled={loading || !llmStatus?.available}
                className="btn-primary flex items-center space-x-2 flex-1"
              >
                {loading ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <Brain className="h-4 w-4" />
                )}
                <span>{loading ? 'Analyzing...' : 'Run Analysis'}</span>
              </button>
              
              <button
                onClick={testLLM}
                disabled={loading}
                className="btn-secondary flex items-center space-x-2"
              >
                <Send className="h-4 w-4" />
                <span>Test</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
          <div className="flex items-center space-x-2">
            <AlertCircle className="h-5 w-5 text-red-600" />
            <p className="text-red-800">{error}</p>
          </div>
        </div>
      )}

      {/* Analysis Results */}
      {analysis && (
        <div className="space-y-6">
          {/* Recommendation Card */}
          <div className="card">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">AI Recommendation</h3>
              <div className="flex items-center space-x-4">
                <span className={`px-3 py-1 rounded-full text-sm font-medium ${getRecommendationColor(analysis.recommendation)}`}>
                  {analysis.recommendation}
                </span>
                <span className={`text-sm font-medium ${getConfidenceColor(analysis.confidence)}`}>
                  {analysis.confidence}% Confidence
                </span>
              </div>
            </div>
            
            <div className="bg-gray-50 p-4 rounded-lg">
              <p className="text-gray-800 font-medium">
                {analysis.recommendation === 'BUY' && '🟢 Buy Recommendation'}
                {analysis.recommendation === 'SELL' && '🔴 Sell Recommendation'}
                {analysis.recommendation === 'HOLD' && '🟡 Hold Recommendation'}
              </p>
              <p className="text-sm text-gray-600 mt-1">
                Based on {days} days of data analysis
              </p>
            </div>
          </div>

          {/* Analysis Text */}
          <div className="card">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Detailed Analysis</h3>
            <div className="prose max-w-none">
              {formatAnalysisText(analysis.analysis)}
            </div>
          </div>

          {/* Technical Summary */}
          {analysis.technicalSummary && (
            <div className="card">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Technical Summary</h3>
              <div className="prose max-w-none">
                {formatAnalysisText(analysis.technicalSummary)}
              </div>
            </div>
          )}

          {/* Analysis Metadata */}
          <div className="card">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Analysis Details</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Symbol</p>
                <p className="font-medium text-gray-900">{analysis.symbol}</p>
              </div>
              <div>
                <p className="text-gray-500">Analysis Type</p>
                <p className="font-medium text-gray-900 capitalize">{analysisType}</p>
              </div>
              <div>
                <p className="text-gray-500">Generated</p>
                <p className="font-medium text-gray-900">
                  {new Date(analysis.timestamp).toLocaleString()}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Data Status */}
      <div className="text-center text-sm text-gray-500">
        <p>
          {stockData.length === 0 
            ? 'No stock data available for analysis'
            : `Ready to analyze ${stockData.length} days of data for ${symbol}`
          }
        </p>
      </div>
    </div>
  );
};

export default LLMAnalysis;
