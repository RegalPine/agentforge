-- AgentForge M3 Durable Runtime - PostgreSQL Schema
-- Reference implementation (logical design)
-- This file defines the schema for production PostgreSQL adapter

-- §31 Execution
CREATE TABLE agentforge_execution (
    execution_id VARCHAR(128) PRIMARY KEY,
    graph_id VARCHAR(255) NOT NULL,
    graph_version VARCHAR(32) NOT NULL,
    plan_fingerprint VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE
);

-- §32 Execution State
CREATE TABLE agentforge_execution_state (
    execution_id VARCHAR(128) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    current_node VARCHAR(255),
    state_data JSONB NOT NULL,
    completed_nodes JSONB NOT NULL,
    waiting_nodes JSONB NOT NULL,
    state_version BIGINT NOT NULL
);

-- §33 Event Table
CREATE TABLE agentforge_execution_event (
    execution_id VARCHAR(128) NOT NULL,
    sequence BIGINT NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    event_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    payload JSONB NOT NULL,

    PRIMARY KEY (
        execution_id,
        sequence
    )
);

-- §34 Checkpoint Table
CREATE TABLE agentforge_execution_checkpoint (
    checkpoint_id VARCHAR(128) PRIMARY KEY,
    execution_id VARCHAR(128) NOT NULL,
    state_version BIGINT NOT NULL,
    current_node VARCHAR(255),
    state_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_agentforge_checkpoint_execution
ON agentforge_execution_checkpoint(
    execution_id,
    state_version DESC
);

-- §35 Invocation Table
CREATE TABLE agentforge_invocation (
    invocation_id VARCHAR(128) PRIMARY KEY,
    execution_id VARCHAR(128) NOT NULL,
    capability_id VARCHAR(255) NOT NULL,
    capability_version VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    result_reference TEXT
);

-- §36 Idempotency Constraint
CREATE UNIQUE INDEX uq_agentforge_invocation_idempotency
ON agentforge_invocation(
    execution_id,
    idempotency_key
);

-- §37 Outbox Table
CREATE TABLE agentforge_outbox (
    message_id VARCHAR(255) PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_agentforge_outbox_unpublished
ON agentforge_outbox(
    published,
    created_at
);

-- Snapshot Table (for recovery optimization)
CREATE TABLE agentforge_execution_snapshot (
    snapshot_id VARCHAR(128) PRIMARY KEY,
    execution_id VARCHAR(128) NOT NULL,
    event_sequence BIGINT NOT NULL,
    state_version BIGINT NOT NULL,
    state_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_agentforge_snapshot_execution
ON agentforge_execution_snapshot(
    execution_id,
    event_sequence DESC
);
