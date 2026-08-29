ALTER TABLE mc_script_execution_task ADD COLUMN approval_id TEXT;
ALTER TABLE mc_script_execution_task ADD COLUMN production INTEGER NOT NULL DEFAULT 0 CHECK (production IN (0, 1));

CREATE TABLE mc_execution_approval (
    id TEXT NOT NULL PRIMARY KEY,
    request_digest TEXT NOT NULL,
    script_id TEXT NOT NULL,
    script_name TEXT NOT NULL,
    service_name TEXT NOT NULL,
    environment TEXT NOT NULL,
    selection_mode TEXT NOT NULL,
    requested_instance_id TEXT,
    requester_id TEXT NOT NULL,
    requester_name TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'CONSUMED')),
    script_content TEXT NOT NULL,
    parameters TEXT,
    reason TEXT NOT NULL,
    approver_id TEXT,
    approver_name TEXT,
    decision_comment TEXT,
    create_time DATETIME NOT NULL,
    expire_time DATETIME NOT NULL,
    decision_time DATETIME,
    consumed_time DATETIME,
    FOREIGN KEY (script_id) REFERENCES mc_script (id)
);

CREATE INDEX idx_approval_status ON mc_execution_approval (status, create_time);
CREATE INDEX idx_approval_requester ON mc_execution_approval (requester_id, create_time DESC);

CREATE TABLE mc_audit_log (
    id TEXT NOT NULL PRIMARY KEY,
    actor_id TEXT NOT NULL,
    actor_name TEXT NOT NULL,
    action TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id TEXT,
    outcome TEXT NOT NULL,
    details TEXT NOT NULL,
    client_ip TEXT,
    user_agent TEXT,
    create_time DATETIME NOT NULL
);

CREATE INDEX idx_audit_actor ON mc_audit_log (actor_id, create_time DESC);
CREATE INDEX idx_audit_target ON mc_audit_log (target_type, target_id, create_time DESC);
CREATE INDEX idx_audit_action ON mc_audit_log (action, create_time DESC);
