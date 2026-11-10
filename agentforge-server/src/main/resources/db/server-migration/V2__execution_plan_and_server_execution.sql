CREATE TABLE execution_plan (
    graph_id VARCHAR(128) NOT NULL,
    graph_version VARCHAR(32) NOT NULL,
    fingerprint VARCHAR(128) NOT NULL,
    plan_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (graph_id, graph_version)
);
CREATE INDEX idx_execution_plan_fingerprint ON execution_plan (fingerprint);

CREATE TABLE server_execution (
    execution_id VARCHAR(128) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    graph_id VARCHAR(128) NOT NULL,
    graph_version VARCHAR(32) NOT NULL,
    plan_fingerprint VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(128),
    inputs_json TEXT NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_server_execution_tenant ON server_execution (tenant_id);
CREATE INDEX idx_server_execution_idempotency ON server_execution (tenant_id, idempotency_key);
CREATE INDEX idx_server_execution_status ON server_execution (tenant_id, status);

CREATE TABLE server_execution_event (
    execution_id VARCHAR(128) NOT NULL,
    sequence BIGINT NOT NULL,
    event_json TEXT NOT NULL,
    PRIMARY KEY (execution_id, sequence)
);
CREATE INDEX idx_server_execution_event_exec ON server_execution_event (execution_id);
