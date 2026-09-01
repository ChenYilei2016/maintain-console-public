CREATE TABLE mc_local_credential (
    user_id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_local_credential_user FOREIGN KEY (user_id) REFERENCES mc_console_user(id),
    UNIQUE KEY uk_local_credential_username(username)
);

