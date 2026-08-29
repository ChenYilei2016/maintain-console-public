CREATE TABLE mc_script_execution_task (
    id TEXT NOT NULL PRIMARY KEY,
    script_id TEXT NOT NULL,
    script_name TEXT NOT NULL,
    service_name TEXT NOT NULL,
    environment TEXT NOT NULL,
    selection_mode TEXT NOT NULL,
    requested_instance_id TEXT,
    executor_id TEXT NOT NULL,
    executor_name TEXT NOT NULL,
    script_content TEXT NOT NULL,
    final_script_content TEXT NOT NULL,
    parameters TEXT,
    status TEXT NOT NULL CHECK (status IN (
        'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'PARTIAL_SUCCESS',
        'CANCELLING', 'CANCELLED', 'TIMED_OUT'
    )),
    targets_json TEXT NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    cancel_requested INTEGER NOT NULL DEFAULT 0 CHECK (cancel_requested IN (0, 1)),
    error_message TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    start_time DATETIME,
    end_time DATETIME,
    duration INTEGER,
    FOREIGN KEY (script_id) REFERENCES mc_script (id)
);

CREATE INDEX idx_execution_task_script ON mc_script_execution_task (script_id, create_time DESC);
CREATE INDEX idx_execution_task_executor ON mc_script_execution_task (executor_id, create_time DESC);
CREATE INDEX idx_execution_task_status ON mc_script_execution_task (status, create_time);
