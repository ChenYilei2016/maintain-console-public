ALTER TABLE mc_script ADD COLUMN parameter_schema TEXT;

ALTER TABLE mc_script_execution_history ADD COLUMN protocol_version INTEGER NOT NULL DEFAULT 1;
ALTER TABLE mc_script_execution_history ADD COLUMN result_payload TEXT;

CREATE TABLE mc_script_revision (
    id TEXT NOT NULL PRIMARY KEY,
    script_id TEXT NOT NULL,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    parameter_schema TEXT,
    permissions TEXT NOT NULL,
    description TEXT,
    creator_id TEXT NOT NULL,
    creator_name TEXT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (script_id, version),
    FOREIGN KEY (script_id) REFERENCES mc_script (id)
);

CREATE INDEX idx_script_revision_script ON mc_script_revision (script_id, version DESC);

INSERT INTO mc_script_revision (
    id, script_id, version, content, parameter_schema, permissions, description,
    creator_id, creator_name, create_time
)
SELECT
    s.id || '-v' || s.version,
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
