-- V2__phase2_schema.sql
-- Digital Emergency Room — Phase 2 Incident + Chaos Schema

-- ============================================================
-- INCIDENTS
-- ============================================================
CREATE TABLE incidents (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id         VARCHAR(50)     NOT NULL UNIQUE,
    session_id          VARCHAR(50)     NOT NULL DEFAULT 'default-session',
    status              VARCHAR(30)     NOT NULL DEFAULT 'CREATED',
    mode                VARCHAR(20)     NOT NULL DEFAULT 'MANUAL',
    severity            VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    affected_service    VARCHAR(50)     NOT NULL,
    traffic_pattern     VARCHAR(30)     NOT NULL DEFAULT 'NORMAL',
    seed                BIGINT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    started_at          TIMESTAMPTZ,
    ended_at            TIMESTAMPTZ
);

CREATE INDEX idx_incidents_status       ON incidents (status);
CREATE INDEX idx_incidents_session      ON incidents (session_id);
CREATE INDEX idx_incidents_created_at   ON incidents (created_at DESC);

-- ============================================================
-- HIDDEN GROUND TRUTH (Internal only - NEVER exposed to AI APIs)
-- ============================================================
CREATE TABLE ground_truth (
    id                  BIGSERIAL       PRIMARY KEY,
    incident_id         VARCHAR(50)     NOT NULL UNIQUE,
    service             VARCHAR(50)     NOT NULL,
    component           VARCHAR(100)    NOT NULL,
    failure_type        VARCHAR(100)    NOT NULL,
    severity            VARCHAR(20)     NOT NULL,
    expected_impact     TEXT,
    traffic_pattern     VARCHAR(30),
    seed                BIGINT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ground_truth_incident  ON ground_truth (incident_id);

-- ============================================================
-- INCIDENT TIMELINE
-- ============================================================
CREATE TABLE incident_timeline (
    id                  BIGSERIAL       PRIMARY KEY,
    incident_id         VARCHAR(50)     NOT NULL,
    timestamp           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    event_type          VARCHAR(50)     NOT NULL,
    service             VARCHAR(50),
    message             TEXT            NOT NULL,
    metadata            TEXT
);

CREATE INDEX idx_timeline_incident      ON incident_timeline (incident_id);
CREATE INDEX idx_timeline_timestamp     ON incident_timeline (timestamp ASC);

-- ============================================================
-- STRUCTURED SYMPTOM LOGS
-- ============================================================
CREATE TABLE simulator_logs (
    id                  BIGSERIAL       PRIMARY KEY,
    timestamp           TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    service             VARCHAR(50)     NOT NULL,
    level               VARCHAR(20)     NOT NULL DEFAULT 'INFO',
    message             TEXT            NOT NULL,
    request_id          VARCHAR(50),
    metadata            TEXT
);

CREATE INDEX idx_logs_timestamp         ON simulator_logs (timestamp DESC);
CREATE INDEX idx_logs_service           ON simulator_logs (service);
CREATE INDEX idx_logs_level             ON simulator_logs (level);

