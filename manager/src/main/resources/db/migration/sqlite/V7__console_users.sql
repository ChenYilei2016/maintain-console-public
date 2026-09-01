CREATE TABLE mc_console_user (
    id TEXT NOT NULL PRIMARY KEY,
    provider TEXT NOT NULL,
    external_subject TEXT NOT NULL,
    employee_no TEXT NOT NULL,
    display_name TEXT NOT NULL,
    roles TEXT NOT NULL DEFAULT '[]',
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'DISABLED')),
    last_login_time DATETIME,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, external_subject),
    UNIQUE (employee_no)
);

CREATE INDEX idx_console_user_status ON mc_console_user(status, id);
