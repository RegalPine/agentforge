-- V2: Add next_event_sequence to execution_state
-- G2-3: Track event sequence in state for crash-recovery monotonic sequences

ALTER TABLE execution_state
    ADD COLUMN IF NOT EXISTS next_event_sequence BIGINT NOT NULL DEFAULT 0;
