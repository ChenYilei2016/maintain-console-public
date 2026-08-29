CREATE TABLE mc_script_user_preference (
    user_id TEXT NOT NULL,
    script_id TEXT NOT NULL,
    favorite INTEGER NOT NULL DEFAULT 0 CHECK (favorite IN (0, 1)),
    last_open_time DATETIME,
    open_count INTEGER NOT NULL DEFAULT 0,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (user_id, script_id),
    FOREIGN KEY (script_id) REFERENCES mc_script (id)
);

CREATE INDEX idx_preference_favorite ON mc_script_user_preference (user_id, favorite, update_time DESC);
CREATE INDEX idx_preference_recent ON mc_script_user_preference (user_id, last_open_time DESC);
