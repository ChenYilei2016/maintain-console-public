CREATE TABLE mc_console_user (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    external_subject VARCHAR(128) NOT NULL,
    employee_no VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    roles TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    last_login_time DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_console_user_provider_subject(provider, external_subject),
    UNIQUE KEY uk_console_user_employee_no(employee_no),
    INDEX idx_console_user_status(status, id)
);
