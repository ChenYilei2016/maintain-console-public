ALTER TABLE mc_script ADD COLUMN tool_metadata TEXT;
ALTER TABLE mc_script_revision ADD COLUMN tool_metadata TEXT;
ALTER TABLE mc_script_execution_history ADD COLUMN environment TEXT;
ALTER TABLE mc_script_execution_history ADD COLUMN script_version INTEGER;
ALTER TABLE mc_script_execution_history ADD COLUMN targets_json TEXT;
ALTER TABLE mc_script_execution_history ADD COLUMN outcome TEXT;
ALTER TABLE mc_script_execution_history ADD COLUMN draft INTEGER;
CREATE INDEX idx_history_script_executor ON mc_script_execution_history(script_id, executor_id, id);
