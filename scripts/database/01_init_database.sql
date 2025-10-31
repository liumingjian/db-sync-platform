-- ================================================
-- Database Initialization Script
-- DB Sync Platform - Metadata Database
-- ================================================

-- Create database (run this as postgres superuser)
-- CREATE DATABASE dbsync_metadata
--     WITH
--     OWNER = dbsync_user
--     ENCODING = 'UTF-8'
--     LC_COLLATE = 'en_US.UTF-8'
--     LC_CTYPE = 'en_US.UTF-8'
--     TEMPLATE = template0;

-- Connect to database
\c dbsync_metadata;

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For text fuzzy search

-- ================================================
-- 1. Tenants Table
-- ================================================

CREATE TABLE IF NOT EXISTS tenants (
    tenant_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Basic Info
    tenant_name VARCHAR(100) NOT NULL,
    tenant_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,

    -- Contact Info
    contact_name VARCHAR(100),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),

    -- Quota Management
    max_connectors INTEGER DEFAULT 10,
    max_tasks_per_connector INTEGER DEFAULT 8,
    max_throughput_tps INTEGER DEFAULT 10000,

    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- Configuration
    config JSONB DEFAULT '{}',

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

-- Indexes
CREATE INDEX idx_tenants_status ON tenants(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_code ON tenants(tenant_code);

-- Comments
COMMENT ON TABLE tenants IS 'Tenant information table';
COMMENT ON COLUMN tenants.tenant_id IS 'Tenant unique identifier';
COMMENT ON COLUMN tenants.config IS 'Tenant-level configuration (JSONB format)';

-- ================================================
-- 2. Sync Tasks Table
-- ================================================

CREATE TABLE IF NOT EXISTS sync_tasks (
    task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Tenant Association
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id) ON DELETE CASCADE,

    -- Task Basic Info
    task_name VARCHAR(200) NOT NULL,
    task_code VARCHAR(100) NOT NULL,
    description TEXT,

    -- Source Database Config
    source_db_type VARCHAR(50) NOT NULL,
    source_connection_config JSONB NOT NULL,

    -- Target Database Config
    target_db_type VARCHAR(50) NOT NULL,
    target_connection_config JSONB NOT NULL,

    -- Debezium Connector Config
    connector_name VARCHAR(200) UNIQUE,
    connector_config JSONB NOT NULL,

    -- Sync Mode
    sync_mode VARCHAR(20) NOT NULL DEFAULT 'FULL_INCREMENTAL',

    -- Task Status
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    health_status VARCHAR(20) DEFAULT 'UNKNOWN',

    -- Error Info
    last_error TEXT,
    error_count INTEGER DEFAULT 0,

    -- Statistics
    total_records_synced BIGINT DEFAULT 0,
    last_sync_time TIMESTAMP,

    -- Alert Config
    alert_config JSONB DEFAULT '{}',

    -- Schedule Config
    schedule_config JSONB DEFAULT '{}',

    -- Audit Fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,

    -- Constraints
    CONSTRAINT uk_tenant_task_code UNIQUE (tenant_id, task_code),
    CONSTRAINT chk_sync_task_source_db_type
        CHECK (source_db_type IN ('MYSQL', 'ORACLE', 'SQLSERVER', 'POSTGRESQL')),
    CONSTRAINT chk_sync_task_target_db_type
        CHECK (target_db_type IN ('MYSQL', 'ORACLE', 'SQLSERVER', 'POSTGRESQL')),
    CONSTRAINT chk_sync_task_mode
        CHECK (sync_mode IN ('FULL_ONLY', 'INCREMENTAL_ONLY', 'FULL_INCREMENTAL')),
    CONSTRAINT chk_sync_task_status
        CHECK (status IN ('CREATED', 'RUNNING', 'PAUSED', 'STOPPED', 'FAILED', 'COMPLETED'))
);

-- Indexes
CREATE INDEX idx_sync_tasks_tenant ON sync_tasks(tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_sync_tasks_status ON sync_tasks(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_sync_tasks_health ON sync_tasks(health_status);
CREATE INDEX idx_sync_tasks_connector ON sync_tasks(connector_name);
CREATE INDEX idx_sync_tasks_source_db ON sync_tasks(source_db_type);
CREATE INDEX idx_sync_tasks_target_db ON sync_tasks(target_db_type);

-- GIN indexes for JSONB
CREATE INDEX idx_sync_tasks_source_config ON sync_tasks USING GIN (source_connection_config);
CREATE INDEX idx_sync_tasks_target_config ON sync_tasks USING GIN (target_connection_config);

-- Comments
COMMENT ON TABLE sync_tasks IS 'Data synchronization task table';
COMMENT ON COLUMN sync_tasks.connector_name IS 'Kafka Connect connector name';
COMMENT ON COLUMN sync_tasks.connector_config IS 'Debezium connector full configuration';

-- ================================================
-- Trigger for updated_at
-- ================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sync_tasks_updated_at BEFORE UPDATE ON sync_tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ================================================
-- Insert Default Tenant
-- ================================================

INSERT INTO tenants (tenant_name, tenant_code, status, description)
VALUES ('Default Tenant', 'default', 'ACTIVE', 'Default tenant for DB Sync Platform')
ON CONFLICT (tenant_code) DO NOTHING;

-- ================================================
-- Success Message
-- ================================================

\echo '================================================'
\echo 'Database initialized successfully!'
\echo '================================================'
