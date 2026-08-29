CREATE TABLE mc_script_user_preference (
    user_id VARCHAR(64) NOT NULL,
    script_id VARCHAR(64) NOT NULL,
    favorite TINYINT(1) NOT NULL DEFAULT 0,
    last_open_time DATETIME(6),
    open_count INT NOT NULL DEFAULT 0,
    update_time DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, script_id),
    CONSTRAINT fk_preference_script FOREIGN KEY (script_id) REFERENCES mc_script (id),
    INDEX idx_preference_favorite (user_id, favorite, update_time),
    INDEX idx_preference_recent (user_id, last_open_time)
);
