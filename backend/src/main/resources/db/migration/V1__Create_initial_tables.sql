-- Stock Genie Database Schema
-- Initial migration to create all tables

-- Create stock_data table
CREATE TABLE IF NOT EXISTS stock_data (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    date DATE NOT NULL,
    open NUMERIC(10,6) NOT NULL,
    high NUMERIC(10,6) NOT NULL,
    low NUMERIC(10,6) NOT NULL,
    close NUMERIC(10,6) NOT NULL,
    volume BIGINT NOT NULL,
    adjusted_close NUMERIC(10,6),
    data_source VARCHAR(50) DEFAULT 'alpha-vantage',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(symbol, date)
);

-- Create technical_analysis table
CREATE TABLE IF NOT EXISTS technical_analysis (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    date DATE NOT NULL,
    indicator_type VARCHAR(50) NOT NULL,
    indicator_value NUMERIC(15,8) NOT NULL,
    period INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(symbol, date, indicator_type, period)
);

-- Create analysis_request table
CREATE TABLE IF NOT EXISTS analysis_request (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL,
    analysis_type VARCHAR(50) NOT NULL,
    request_data JSONB,
    response_data JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_stock_data_symbol_date ON stock_data(symbol, date);
CREATE INDEX IF NOT EXISTS idx_stock_data_date ON stock_data(date);
CREATE INDEX IF NOT EXISTS idx_stock_data_symbol ON stock_data(symbol);

CREATE INDEX IF NOT EXISTS idx_technical_analysis_symbol_date ON technical_analysis(symbol, date);
CREATE INDEX IF NOT EXISTS idx_technical_analysis_indicator ON technical_analysis(indicator_type);
CREATE INDEX IF NOT EXISTS idx_technical_analysis_symbol_indicator ON technical_analysis(symbol, indicator_type);

CREATE INDEX IF NOT EXISTS idx_analysis_request_symbol ON analysis_request(symbol);
CREATE INDEX IF NOT EXISTS idx_analysis_request_status ON analysis_request(status);
CREATE INDEX IF NOT EXISTS idx_analysis_request_created_at ON analysis_request(created_at);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_stock_data_updated_at 
    BEFORE UPDATE ON stock_data 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_analysis_request_updated_at 
    BEFORE UPDATE ON analysis_request 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create view for latest stock data
CREATE OR REPLACE VIEW latest_stock_data AS
SELECT DISTINCT ON (symbol) 
    symbol,
    date,
    open,
    high,
    low,
    close,
    volume,
    adjusted_close,
    data_source,
    created_at,
    updated_at
FROM stock_data 
ORDER BY symbol, date DESC;

-- Create view for stock data summary
CREATE OR REPLACE VIEW stock_data_summary AS
SELECT 
    symbol,
    COUNT(*) as total_records,
    MIN(date) as earliest_date,
    MAX(date) as latest_date,
    AVG(close) as avg_close,
    MIN(close) as min_close,
    MAX(close) as max_close,
    SUM(volume) as total_volume
FROM stock_data 
GROUP BY symbol;

-- Insert sample data (optional - for testing)
-- INSERT INTO stock_data (symbol, date, open, high, low, close, volume, adjusted_close, data_source) VALUES
-- ('AAPL', '2024-01-01', 150.00, 155.00, 149.00, 154.00, 1000000, 154.00, 'sample'),
-- ('MSFT', '2024-01-01', 300.00, 305.00, 299.00, 304.00, 800000, 304.00, 'sample');

-- Create comments for documentation
COMMENT ON TABLE stock_data IS 'Historical stock price data';
COMMENT ON TABLE technical_analysis IS 'Technical analysis indicators for stocks';
COMMENT ON TABLE analysis_request IS 'AI analysis requests and responses';

COMMENT ON COLUMN stock_data.symbol IS 'Stock symbol (e.g., AAPL, MSFT)';
COMMENT ON COLUMN stock_data.date IS 'Trading date';
COMMENT ON COLUMN stock_data.open IS 'Opening price';
COMMENT ON COLUMN stock_data.high IS 'Highest price of the day';
COMMENT ON COLUMN stock_data.low IS 'Lowest price of the day';
COMMENT ON COLUMN stock_data.close IS 'Closing price';
COMMENT ON COLUMN stock_data.volume IS 'Trading volume';
COMMENT ON COLUMN stock_data.adjusted_close IS 'Adjusted closing price';
COMMENT ON COLUMN stock_data.data_source IS 'Source of the data (alpha-vantage, eodhd, etc.)';

COMMENT ON COLUMN technical_analysis.indicator_type IS 'Type of technical indicator (SMA, EMA, RSI, etc.)';
COMMENT ON COLUMN technical_analysis.indicator_value IS 'Value of the technical indicator';
COMMENT ON COLUMN technical_analysis.period IS 'Period used for calculation (e.g., 20 for SMA_20)';
