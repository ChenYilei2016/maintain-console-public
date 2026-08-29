CREATE TABLE IF NOT EXISTS mc_directory_node (
    id VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    parent_id VARCHAR(64),
    service_name VARCHAR(100) NOT NULL,
    sort_order INT DEFAULT 0,
    creator_id VARCHAR(50) NOT NULL,
    creator_name VARCHAR(100) NOT NULL,
    permission_type VARCHAR(20) NOT NULL DEFAULT 'public',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_directory_tree_query (service_name, parent_id, type, is_deleted, sort_order),
    KEY idx_service_creator (service_name, creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mc_script (
    id VARCHAR(64) NOT NULL,
    content LONGTEXT NOT NULL,
    permissions JSON NOT NULL,
    description VARCHAR(500),
    version INT DEFAULT 1,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mc_script_execution_history (
    id VARCHAR(64) NOT NULL,
    script_id VARCHAR(64) NOT NULL,
    script_name VARCHAR(200) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    executor_id VARCHAR(50) NOT NULL,
    executor_name VARCHAR(100) NOT NULL,
    script_content LONGTEXT NOT NULL,
    parameters TEXT,
    final_script_content LONGTEXT,
    result LONGTEXT,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration INT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_history_script_id (script_id),
    KEY idx_history_executor (service_name, executor_id),
    KEY idx_history_start_time (start_time),
    KEY idx_history_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
