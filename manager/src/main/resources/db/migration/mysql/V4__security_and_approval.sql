ALTER TABLE mc_script_execution_task
    ADD COLUMN approval_id VARCHAR(64),
    ADD COLUMN production TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE mc_execution_approval (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    request_digest VARCHAR(64) NOT NULL,
    script_id VARCHAR(64) NOT NULL,
    script_name VARCHAR(255) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    environment VARCHAR(64) NOT NULL,
    selection_mode VARCHAR(32) NOT NULL,
    requested_instance_id VARCHAR(255),
    requester_id VARCHAR(64) NOT NULL,
    requester_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    script_content MEDIUMTEXT NOT NULL,
    parameters MEDIUMTEXT,
    reason VARCHAR(1000) NOT NULL,
    approver_id VARCHAR(64),
    approver_name VARCHAR(255),
    decision_comment VARCHAR(1000),
    create_time DATETIME(6) NOT NULL,
    expire_time DATETIME(6) NOT NULL,
    decision_time DATETIME(6),
    consumed_time DATETIME(6),
    CONSTRAINT fk_approval_script FOREIGN KEY (script_id) REFERENCES mc_script (id),
    INDEX idx_approval_status (status, create_time),
    INDEX idx_approval_requester (requester_id, create_time DESC)
);

CREATE TABLE mc_audit_log (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    actor_id VARCHAR(64) NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(64),
    outcome VARCHAR(32) NOT NULL,
    details TEXT NOT NULL,
    client_ip VARCHAR(128),
    user_agent VARCHAR(512),
    create_time DATETIME(6) NOT NULL,
    INDEX idx_audit_actor (actor_id, create_time),
    INDEX idx_audit_target (target_type, target_id, create_time),
    INDEX idx_audit_action (action, create_time)
);
