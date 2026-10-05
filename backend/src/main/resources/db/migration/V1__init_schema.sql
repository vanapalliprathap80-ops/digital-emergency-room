-- V1__init_schema.sql
-- Digital Emergency Room — Phase 1 initial schema

-- ============================================================
-- ORDERS
-- ============================================================
CREATE TABLE orders (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(100)    NOT NULL,
    amount      NUMERIC(12, 2)  NOT NULL CHECK (amount > 0),
    status      VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_created_at  ON orders (created_at DESC);
CREATE INDEX idx_orders_user_id     ON orders (user_id);
CREATE INDEX idx_orders_status      ON orders (status);

-- ============================================================
-- TELEMETRY
-- ============================================================
CREATE TABLE telemetry (
    id          BIGSERIAL       PRIMARY KEY,
    request_id  VARCHAR(50)     NOT NULL,
    timestamp   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    service     VARCHAR(50)     NOT NULL,
    operation   VARCHAR(100)    NOT NULL,
    endpoint    VARCHAR(255),
    http_method VARCHAR(10),
    status_code INTEGER,
    success     BOOLEAN         NOT NULL DEFAULT TRUE,
    latency_ms  BIGINT          NOT NULL DEFAULT 0,
    error_type  VARCHAR(100),
    message     TEXT
);

CREATE INDEX idx_telemetry_timestamp   ON telemetry (timestamp DESC);
CREATE INDEX idx_telemetry_request_id  ON telemetry (request_id);
CREATE INDEX idx_telemetry_service     ON telemetry (service);
CREATE INDEX idx_telemetry_success     ON telemetry (success);

-- ============================================================
-- APPLICATION EVENTS
-- ============================================================
CREATE TABLE application_events (
    id          BIGSERIAL       PRIMARY KEY,
    event_id    VARCHAR(50)     NOT NULL UNIQUE,
    timestamp   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    request_id  VARCHAR(50),
    event_type  VARCHAR(100)    NOT NULL,
    service     VARCHAR(50)     NOT NULL,
    message     TEXT,
    metadata    TEXT
);

CREATE INDEX idx_events_timestamp   ON application_events (timestamp DESC);
CREATE INDEX idx_events_request_id  ON application_events (request_id);
CREATE INDEX idx_events_type        ON application_events (event_type);
