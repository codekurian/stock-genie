import React, { useState, useEffect } from 'react';
import { CheckCircle, AlertCircle, RefreshCw } from 'lucide-react';
import { healthApi } from '../services/api';

const HealthStatus: React.FC = () => {
  const [health, setHealth] = useState<{ status: string; message: string } | null>(null);
  const [loading, setLoading] = useState(false);

  const checkHealth = async () => {
    setLoading(true);
    try {
      const response = await healthApi.getHealth();
      setHealth(response);
    } catch (err) {
      setHealth({ status: 'DOWN', message: 'Service unavailable' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkHealth();
    // Check health every 30 seconds
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  const isHealthy = health?.status === 'UP';

  return (
    <div className="flex items-center space-x-2">
      <button
        onClick={checkHealth}
        disabled={loading}
        className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm font-medium transition-colors ${
          isHealthy
            ? 'bg-green-100 text-green-800 hover:bg-green-200'
            : 'bg-red-100 text-red-800 hover:bg-red-200'
        }`}
      >
        {loading ? (
          <RefreshCw className="h-4 w-4 animate-spin" />
        ) : isHealthy ? (
          <CheckCircle className="h-4 w-4" />
        ) : (
          <AlertCircle className="h-4 w-4" />
        )}
        <span>{isHealthy ? 'Online' : 'Offline'}</span>
      </button>
    </div>
  );
};

export default HealthStatus;
