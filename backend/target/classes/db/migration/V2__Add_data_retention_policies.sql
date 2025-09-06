-- Data Retention Policies
-- This migration adds data retention policies and cleanup procedures

-- Create function to clean up old data
CREATE OR REPLACE FUNCTION cleanup_old_data(retention_days INTEGER DEFAULT 365)
RETURNS TABLE(
    table_name TEXT,
    deleted_count BIGINT
) AS $$
DECLARE
    cutoff_date DATE;
    stock_data_count BIGINT;
    technical_analysis_count BIGINT;
    analysis_request_count BIGINT;
BEGIN
    cutoff_date := CURRENT_DATE - retention_days;
    
    -- Clean up old stock data
    DELETE FROM stock_data WHERE date < cutoff_date;
    GET DIAGNOSTICS stock_data_count = ROW_COUNT;
    
    -- Clean up old technical analysis data
    DELETE FROM technical_analysis WHERE date < cutoff_date;
    GET DIAGNOSTICS technical_analysis_count = ROW_COUNT;
    
    -- Clean up old analysis requests (keep for 30 days)
    DELETE FROM analysis_request WHERE created_at < (CURRENT_TIMESTAMP - INTERVAL '30 days');
    GET DIAGNOSTICS analysis_request_count = ROW_COUNT;
    
    -- Return cleanup statistics
    RETURN QUERY SELECT 'stock_data'::TEXT, stock_data_count;
    RETURN QUERY SELECT 'technical_analysis'::TEXT, technical_analysis_count;
    RETURN QUERY SELECT 'analysis_request'::TEXT, analysis_request_count;
END;
$$ LANGUAGE plpgsql;

-- Create function to get database statistics
CREATE OR REPLACE FUNCTION get_database_stats()
RETURNS TABLE(
    table_name TEXT,
    record_count BIGINT,
    oldest_record DATE,
    newest_record DATE,
    table_size TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'stock_data'::TEXT,
        COUNT(*),
        MIN(date),
        MAX(date),
        pg_size_pretty(pg_total_relation_size('stock_data'))
    FROM stock_data
    UNION ALL
    SELECT 
        'technical_analysis'::TEXT,
        COUNT(*),
        MIN(date),
        MAX(date),
        pg_size_pretty(pg_total_relation_size('technical_analysis'))
    FROM technical_analysis
    UNION ALL
    SELECT 
        'analysis_request'::TEXT,
        COUNT(*),
        MIN(created_at::DATE),
        MAX(created_at::DATE),
        pg_size_pretty(pg_total_relation_size('analysis_request'))
    FROM analysis_request;
END;
$$ LANGUAGE plpgsql;

-- Create function to optimize database performance
CREATE OR REPLACE FUNCTION optimize_database()
RETURNS TEXT AS $$
BEGIN
    -- Update table statistics
    ANALYZE stock_data;
    ANALYZE technical_analysis;
    ANALYZE analysis_request;
    
    -- Vacuum tables to reclaim space
    VACUUM stock_data;
    VACUUM technical_analysis;
    VACUUM analysis_request;
    
    RETURN 'Database optimization completed successfully';
END;
$$ LANGUAGE plpgsql;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_stock_data_date_range ON stock_data(date) WHERE date >= CURRENT_DATE - INTERVAL '1 year';
CREATE INDEX IF NOT EXISTS idx_technical_analysis_date_range ON technical_analysis(date) WHERE date >= CURRENT_DATE - INTERVAL '1 year';

-- Create partial indexes for active data
CREATE INDEX IF NOT EXISTS idx_stock_data_recent ON stock_data(symbol, date) WHERE date >= CURRENT_DATE - INTERVAL '30 days';
CREATE INDEX IF NOT EXISTS idx_technical_analysis_recent ON technical_analysis(symbol, date, indicator_type) WHERE date >= CURRENT_DATE - INTERVAL '30 days';

-- Create function to get stock data with pagination
CREATE OR REPLACE FUNCTION get_stock_data_paginated(
    p_symbol VARCHAR(10),
    p_start_date DATE,
    p_end_date DATE,
    p_limit INTEGER DEFAULT 100,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE(
    id BIGINT,
    symbol VARCHAR(10),
    date DATE,
    open NUMERIC(10,6),
    high NUMERIC(10,6),
    low NUMERIC(10,6),
    close NUMERIC(10,6),
    volume BIGINT,
    adjusted_close NUMERIC(10,6),
    data_source VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        sd.id,
        sd.symbol,
        sd.date,
        sd.open,
        sd.high,
        sd.low,
        sd.close,
        sd.volume,
        sd.adjusted_close,
        sd.data_source,
        sd.created_at,
        sd.updated_at
    FROM stock_data sd
    WHERE sd.symbol = p_symbol
        AND sd.date >= p_start_date
        AND sd.date <= p_end_date
    ORDER BY sd.date DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Create function to get technical analysis with pagination
CREATE OR REPLACE FUNCTION get_technical_analysis_paginated(
    p_symbol VARCHAR(10),
    p_indicator_type VARCHAR(50),
    p_start_date DATE,
    p_end_date DATE,
    p_limit INTEGER DEFAULT 100,
    p_offset INTEGER DEFAULT 0
)
RETURNS TABLE(
    id BIGINT,
    symbol VARCHAR(10),
    date DATE,
    indicator_type VARCHAR(50),
    indicator_value NUMERIC(15,8),
    period INTEGER,
    created_at TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ta.id,
        ta.symbol,
        ta.date,
        ta.indicator_type,
        ta.indicator_value,
        ta.period,
        ta.created_at
    FROM technical_analysis ta
    WHERE ta.symbol = p_symbol
        AND ta.indicator_type = p_indicator_type
        AND ta.date >= p_start_date
        AND ta.date <= p_end_date
    ORDER BY ta.date DESC
    LIMIT p_limit
    OFFSET p_offset;
END;
$$ LANGUAGE plpgsql;

-- Add comments for new functions
COMMENT ON FUNCTION cleanup_old_data(INTEGER) IS 'Cleans up old data based on retention policy';
COMMENT ON FUNCTION get_database_stats() IS 'Returns database statistics and table sizes';
COMMENT ON FUNCTION optimize_database() IS 'Optimizes database performance by analyzing and vacuuming tables';
COMMENT ON FUNCTION get_stock_data_paginated(VARCHAR, DATE, DATE, INTEGER, INTEGER) IS 'Gets stock data with pagination support';
COMMENT ON FUNCTION get_technical_analysis_paginated(VARCHAR, VARCHAR, DATE, DATE, INTEGER, INTEGER) IS 'Gets technical analysis data with pagination support';
