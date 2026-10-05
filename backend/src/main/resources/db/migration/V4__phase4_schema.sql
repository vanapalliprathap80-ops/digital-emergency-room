CREATE TABLE remediation_actions (
    action_id VARCHAR(50) PRIMARY KEY,
    incident_id VARCHAR(50) NOT NULL,
    investigation_id VARCHAR(50),
    action_type VARCHAR(60) NOT NULL,
    target_service VARCHAR(50),
    parameters TEXT,
    risk_level VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    proposed_at TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    executed_at TIMESTAMP,
    completed_at TIMESTAMP,
    proposed_by VARCHAR(50),
    approved_by VARCHAR(50),
    validation_result TEXT,
    before_snapshot TEXT,
    after_snapshot TEXT,
    execution_result TEXT,
    failure_reason TEXT,
    reason TEXT,
    expected_impact TEXT,
    rollback_available BOOLEAN,
    attempt_number INT NOT NULL DEFAULT 1,
    verification_result TEXT
);

CREATE INDEX idx_remediation_incident ON remediation_actions(incident_id);
