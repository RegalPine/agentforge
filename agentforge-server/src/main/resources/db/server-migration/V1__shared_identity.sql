-- 服务端身份存储的独立迁移历史；不改写已有执行数据库迁移。
CREATE TABLE iam_bootstrap_lock (
    lock_id INTEGER PRIMARY KEY
);
INSERT INTO iam_bootstrap_lock(lock_id) VALUES (1);

CREATE TABLE iam_tenant (
    tenant_id VARCHAR(128) PRIMARY KEY,
    tenant_name VARCHAR(255) NOT NULL UNIQUE,
    record_json TEXT NOT NULL
);
CREATE TABLE iam_user (
    user_id VARCHAR(128) PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320),
    external_subject VARCHAR(255),
    external_issuer VARCHAR(512),
    record_json TEXT NOT NULL,
    UNIQUE (external_subject, external_issuer)
);
CREATE TABLE iam_role (
    role_id VARCHAR(128) PRIMARY KEY,
    tenant_id VARCHAR(128) REFERENCES iam_tenant(tenant_id) ON DELETE CASCADE,
    record_json TEXT NOT NULL
);
CREATE TABLE iam_role_assignment (
    user_id VARCHAR(128) NOT NULL REFERENCES iam_user(user_id) ON DELETE CASCADE,
    role_id VARCHAR(128) NOT NULL REFERENCES iam_role(role_id) ON DELETE CASCADE,
    tenant_scope VARCHAR(128) NOT NULL,
    PRIMARY KEY (user_id, role_id, tenant_scope)
);
CREATE TABLE iam_session (
    token_hash CHAR(64) PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL REFERENCES iam_user(user_id) ON DELETE CASCADE,
    tenant_id VARCHAR(128) NOT NULL REFERENCES iam_tenant(tenant_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CHECK (expires_at > created_at)
);
CREATE INDEX idx_iam_session_user ON iam_session(user_id);
CREATE INDEX idx_iam_session_expiry ON iam_session(expires_at);
