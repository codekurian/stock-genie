-- PostgreSQL-specific migration to add JSONB support
-- This migration only runs on PostgreSQL databases

-- Check if we're running on PostgreSQL
DO $$
BEGIN
    -- Only run this migration on PostgreSQL
    IF current_setting('server_version_num')::int >= 90000 THEN
        -- Alter the analysis_request table to use JSONB instead of TEXT
        -- This is a no-op if the columns are already JSONB
        BEGIN
            ALTER TABLE analysis_request 
            ALTER COLUMN request_data TYPE JSONB USING request_data::JSONB;
        EXCEPTION
            WHEN OTHERS THEN
                -- Column might already be JSONB or conversion might fail
                NULL;
        END;
        
        BEGIN
            ALTER TABLE analysis_request 
            ALTER COLUMN response_data TYPE JSONB USING response_data::JSONB;
        EXCEPTION
            WHEN OTHERS THEN
                -- Column might already be JSONB or conversion might fail
                NULL;
        END;
    END IF;
END $$;
