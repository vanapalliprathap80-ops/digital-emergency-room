-- V3__phase3_schema.sql
-- Digital Emergency Room - Phase 3 AI Investigation Schema

-- ============================================================
-- INVESTIGATIONS
-- ============================================================
CREATE TABLE investigations (
    id                  VARCHAR(50)     PRIMARY KEY,
    incident_id         VARCHAR(50)     NOT NULL,
    engine              VARCHAR(50)     NOT NULL DEFAULT 'GEMINI',
    status              VARCHAR(30)     NOT NULL DEFAULT 'RUNNING',
    started_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    tool_call_count     INT             NOT NULL DEFAULT 0,
    diagnosis_status    VARCHAR(30),
    root_cause_service  VARCHAR(50),
    root_cause_component VARCHAR(100),
    root_cause_failure  VARCHAR(100),
    confidence          VARCHAR(20),
    impact              TEXT,
    recommended_action  TEXT
);

CREATE INDEX idx_investigations_incident ON investigations (incident_id);

-- ============================================================
-- INVESTIGATION EVIDENCE
-- ============================================================
CREATE TABLE investigation_evidence (
    id                  BIGSERIAL       PRIMARY KEY,
    investigation_id    VARCHAR(50)     NOT NULL,
    source              VARCHAR(100)    NOT NULL,
    observation         TEXT            NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evidence_investigation ON investigation_evidence (investigation_id);

-- ============================================================
-- INVESTIGATION TOOL CALLS (AUDIT)
-- ============================================================
CREATE TABLE investigation_tool_calls (
    id                  VARCHAR(50)     PRIMARY KEY,
    investigation_id    VARCHAR(50)     NOT NULL,
    tool_name           VARCHAR(100)    NOT NULL,
    input_args          TEXT,
    output_summary      TEXT,
    success             BOOLEAN         NOT NULL DEFAULT TRUE,
    duration_ms         BIGINT          NOT NULL DEFAULT 0,
    timestamp           TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tool_calls_investigation ON investigation_tool_calls (investigation_id);

-- ============================================================
-- EVALUATION REPORTS
-- ============================================================
CREATE TABLE evaluations (
    investigation_id    VARCHAR(50)     PRIMARY KEY,
    service_correct     BOOLEAN,
    component_correct   BOOLEAN,
    failure_correct     BOOLEAN,
    severity_correct    BOOLEAN
);
