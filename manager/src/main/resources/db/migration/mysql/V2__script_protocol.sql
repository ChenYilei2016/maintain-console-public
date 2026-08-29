ALTER TABLE mc_script ADD COLUMN parameter_schema JSON NULL AFTER content;

ALTER TABLE mc_script_execution_history
    ADD COLUMN protocol_version INT NOT NULL DEFAULT 1 AFTER result,
    ADD COLUMN result_payload LONGTEXT NULL AFTER protocol_version;

CREATE TABLE mc_script_revision (
    id VARCHAR(64) NOT NULL,
    script_id VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    content LONGTEXT NOT NULL,
    parameter_schema JSON,
    permissions JSON NOT NULL,
    description VARCHAR(500),
    creator_id VARCHAR(50) NOT NULL,
    creator_name VARCHAR(100) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_script_revision (script_id, version),
    KEY idx_script_revision_script (script_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO mc_script_revision (
    id, script_id, version, content, parameter_schema, permissions, description,
    creator_id, creator_name, create_time
)
SELECT
    CONCAT(s.id, '-v', s.version),
    s.id,
    s.version,
    s.content,
    s.parameter_schema,
    s.permissions,
    s.description,
    n.creator_id,
    n.creator_name,
    s.update_time
FROM mc_script s
JOIN mc_directory_node n ON n.id = s.id;
