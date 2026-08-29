CREATE TABLE IF NOT EXISTS mc_directory_node (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('folder', 'script')),
    parent_id TEXT,
    service_name TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0,
    creator_id TEXT NOT NULL,
    creator_name TEXT NOT NULL,
    permission_type TEXT NOT NULL DEFAULT 'public' CHECK (permission_type IN ('public', 'private')),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted INTEGER DEFAULT 0 CHECK (is_deleted IN (0, 1)),
    FOREIGN KEY (parent_id) REFERENCES mc_directory_node (id)
);

CREATE INDEX IF NOT EXISTS idx_directory_tree_query
    ON mc_directory_node (service_name, parent_id, type, is_deleted, sort_order);
CREATE INDEX IF NOT EXISTS idx_service_creator ON mc_directory_node (service_name, creator_id);
CREATE INDEX IF NOT EXISTS idx_permission_type ON mc_directory_node (permission_type);

CREATE TABLE IF NOT EXISTS mc_script (
    id TEXT NOT NULL PRIMARY KEY,
    content TEXT NOT NULL,
    permissions TEXT NOT NULL,
    description TEXT,
    version INTEGER DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id) REFERENCES mc_directory_node (id)
);

CREATE TABLE IF NOT EXISTS mc_script_execution_history (
    id TEXT NOT NULL PRIMARY KEY,
    script_id TEXT NOT NULL,
    script_name TEXT NOT NULL,
    service_name TEXT NOT NULL,
    executor_id TEXT NOT NULL,
    executor_name TEXT NOT NULL,
    script_content TEXT NOT NULL,
    parameters TEXT,
    final_script_content TEXT,
    result TEXT,
    status TEXT NOT NULL CHECK (status IN ('success', 'error', 'running')),
    error_message TEXT,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration INTEGER,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (script_id) REFERENCES mc_script (id)
);

CREATE INDEX IF NOT EXISTS idx_history_script_id ON mc_script_execution_history (script_id);
CREATE INDEX IF NOT EXISTS idx_history_executor ON mc_script_execution_history (service_name, executor_id);
CREATE INDEX IF NOT EXISTS idx_history_start_time ON mc_script_execution_history (start_time);
CREATE INDEX IF NOT EXISTS idx_history_status ON mc_script_execution_history (status);
