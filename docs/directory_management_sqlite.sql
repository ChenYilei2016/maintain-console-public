-- 目录管理相关表结构 - SQLite 版本

-- 1. 目录节点表（文件夹和脚本都在这个表中）
drop table if exists `mc_directory_node`;
CREATE TABLE `mc_directory_node` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `name` TEXT NOT NULL,
    `type` TEXT NOT NULL CHECK(`type` IN ('folder', 'script')), -- 节点类型: folder-文件夹, script-脚本
    `parent_id` TEXT DEFAULT NULL,
    `service_name` TEXT NOT NULL,
    `sort_order` INTEGER DEFAULT 0,
    `creator_id` TEXT NOT NULL,
    `creator_name` TEXT NOT NULL,
    `permission_type` TEXT NOT NULL DEFAULT 'public' CHECK(`permission_type` IN ('public', 'private')), -- 权限类型: public-公共, private-私有，文件夹默认public，脚本默认public但可设private
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `is_deleted` INTEGER DEFAULT 0 CHECK(`is_deleted` IN (0, 1)),
    FOREIGN KEY (`parent_id`) REFERENCES `mc_directory_node`(`id`)
);

-- 创建索引
CREATE INDEX `idx_parent_service` ON `mc_directory_node`(`parent_id`, `service_name`);
CREATE INDEX `idx_service_creator` ON `mc_directory_node`(`service_name`, `creator_id`);
CREATE INDEX `idx_type` ON `mc_directory_node`(`type`);
CREATE INDEX `idx_create_time` ON `mc_directory_node`(`create_time`);

-- 2. 脚本内容表（存储脚本的具体内容和权限配置）
drop table if exists `mc_script`;
CREATE TABLE `mc_script` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `content` TEXT NOT NULL,
    `permissions` TEXT NOT NULL, -- SQLite 使用 TEXT 存储 JSON
    `description` TEXT DEFAULT NULL,
    `version` INTEGER DEFAULT 1,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`id`) REFERENCES `mc_directory_node`(`id`)
);

-- 创建索引
CREATE INDEX `idx_script_update_time` ON `mc_script`(`update_time`);

-- 3. 脚本执行历史表
drop table if exists `mc_script_execution_history`;
CREATE TABLE `mc_script_execution_history` (
    `id` TEXT NOT NULL PRIMARY KEY,
    `script_id` TEXT NOT NULL,
    `script_name` TEXT NOT NULL,
    `service_name` TEXT NOT NULL,
    `executor_id` TEXT NOT NULL,
    `executor_name` TEXT NOT NULL,
    `script_content` TEXT NOT NULL,
    `parameters` TEXT,
    `final_script_content` TEXT ,
    `result` TEXT,
    `status` TEXT NOT NULL CHECK(`status` IN ('success', 'error', 'running')),
    `error_message` TEXT,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME DEFAULT NULL,
    `duration` INTEGER DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`script_id`) REFERENCES `mc_script`(`id`)
);

-- 创建索引
CREATE INDEX `idx_script_id` ON `mc_script_execution_history`(`script_id`);
CREATE INDEX `idx_service_executor` ON `mc_script_execution_history`(`service_name`, `executor_id`);
CREATE INDEX `idx_start_time` ON `mc_script_execution_history`(`start_time`);
CREATE INDEX `idx_status` ON `mc_script_execution_history`(`status`);

-- 创建触发器来自动更新 update_time
CREATE TRIGGER update_mc_directory_node_timestamp 
    AFTER UPDATE ON mc_directory_node
    FOR EACH ROW
    WHEN OLD.update_time = NEW.update_time OR OLD.update_time IS NULL
BEGIN
    UPDATE mc_directory_node SET update_time=CURRENT_TIMESTAMP WHERE id=NEW.id;
END;

CREATE TRIGGER update_mc_script_timestamp 
    AFTER UPDATE ON mc_script
    FOR EACH ROW
    WHEN OLD.update_time = NEW.update_time OR OLD.update_time IS NULL
BEGIN
    UPDATE mc_script SET update_time=CURRENT_TIMESTAMP WHERE id=NEW.id;
END;

-- 创建复合索引优化查询性能
CREATE INDEX `idx_directory_tree_query` ON `mc_directory_node`(`service_name`, `parent_id`, `type`, `is_deleted`, `sort_order`);
CREATE INDEX `idx_permission_type` ON `mc_directory_node`(`permission_type`);

-- SQLite 特有的配置
PRAGMA foreign_keys = ON;  -- 启用外键约束
PRAGMA journal_mode = WAL; -- 启用 WAL 模式提高并发性能