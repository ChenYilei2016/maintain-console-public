ALTER TABLE mc_script ADD COLUMN tool_metadata TEXT;
ALTER TABLE mc_script_revision ADD COLUMN tool_metadata TEXT;
ALTER TABLE mc_script_execution_history ADD COLUMN environment VARCHAR(128);
ALTER TABLE mc_script_execution_history ADD COLUMN script_version INT;
ALTER TABLE mc_script_execution_history ADD COLUMN targets_json MEDIUMTEXT;
ALTER TABLE mc_script_execution_history ADD COLUMN outcome VARCHAR(32);
ALTER TABLE mc_script_execution_history ADD COLUMN draft BOOLEAN;
CREATE INDEX idx_history_script_executor ON mc_script_execution_history(script_id, executor_id, id);
