-- 目录管理相关表结构

-- 1. 目录节点表（文件夹和脚本都在这个表中）
drop table if exists `mc_directory_node`
CREATE TABLE `mc_directory_node` (
    `id` varchar(64) NOT NULL COMMENT '节点ID',
    `name` varchar(200) NOT NULL COMMENT '节点名称',
    `type` varchar(20) NOT NULL COMMENT '节点类型: folder-文件夹, script-脚本',
    `parent_id` varchar(64) DEFAULT NULL COMMENT '父节点ID',
    `service_name` varchar(100) NOT NULL COMMENT '应用服务名',
    `permission_type` varchar(100) NOT NULL COMMENT '权限类型: public-公共, private-私有，文件夹默认public，脚本默认public但可设private',
    `sort_order` bigint DEFAULT 0 COMMENT '排序字段',
    `creator_id` varchar(50) NOT NULL COMMENT '创建人ID',
    `creator_name` varchar(100) NOT NULL COMMENT '创建人姓名',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_parent_service` (`parent_id`, `service_name`),
    KEY `idx_service_creator` (`service_name`, `creator_id`),
    KEY `idx_type` (`type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目录节点表';

-- 2. 脚本内容表（存储脚本的具体内容和权限配置）
drop table if exists `mc_script`
CREATE TABLE `mc_script` (
    `id` varchar(64) NOT NULL COMMENT '脚本ID（对应directory_node的id）',
    `content` text NOT NULL COMMENT '脚本内容',
    `permissions` json not null COMMENT '权限配置JSON',
    `description` varchar(500) DEFAULT NULL COMMENT '脚本描述',
    `version` int DEFAULT 1 COMMENT '版本号',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='脚本内容表';

-- 3. 脚本执行历史表
drop table if exists `mc_script_execution_history`;
CREATE TABLE `mc_script_execution_history` (
    `id` varchar(64) NOT NULL COMMENT '执行记录ID',
    `script_id` varchar(64) NOT NULL COMMENT '脚本ID',
    `script_name` varchar(200) NOT NULL COMMENT '脚本名称',
    `service_name` varchar(100) NOT NULL COMMENT '执行的服务名',
    `executor_id` varchar(50) NOT NULL COMMENT '执行人 ID',
    `executor_name` varchar(100) NOT NULL COMMENT '执行人姓名',
    `script_content` longtext NOT NULL COMMENT '执行时的脚本内容',
    `parameters` text COMMENT '执行参数JSON',
    `final_script_content` longtext COMMENT '执行时的脚本内容',
    `result` text COMMENT '执行结果',
    `status` varchar(20) NOT NULL COMMENT '执行状态: success-成功, error-失败, running-运行中',
    `error_message` text COMMENT '错误信息',
    `start_time` datetime NOT NULL COMMENT '开始执行时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束执行时间',
    `duration` int DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_script_id` (`script_id`),
    KEY `idx_service_executor` (`service_name`, `executor_id`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='脚本执行历史表';
--
-- -- 4. 目录权限表（可扩展，用于更细粒度的权限控制）
-- CREATE TABLE `mc_directory_permission` (
--     `id` varchar(64) NOT NULL COMMENT '权限ID',
--     `node_id` varchar(64) NOT NULL COMMENT '节点ID',
--     `permission_type` varchar(20) NOT NULL COMMENT '权限类型: read-读取, write-编辑, execute-执行, delete-删除',
--     `user_type` varchar(20) NOT NULL COMMENT '用户类型: user-指定用户, role-角色, all-所有人',
--     `user_value` varchar(100) DEFAULT NULL COMMENT '用户值（工号/角色代码等）',
--     `creator_id` varchar(50) NOT NULL COMMENT '创建人 ID',
--     `creator_name` varchar(100) NOT NULL COMMENT '创建人姓名',
--     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
--     PRIMARY KEY (`id`),
--     KEY `idx_node_id` (`node_id`),
--     KEY `idx_permission_type` (`permission_type`),
--     KEY `idx_user_type_value` (`user_type`, `user_value`)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='目录权限表';

-- -- 初始化默认根目录数据
-- INSERT INTO `mc_directory_node` (`id`, `name`, `type`, `parent_id`, `service_name`, `sort_order`, `creator_id`, `creator_name`, `create_time`, `update_time`) VALUES
-- ('my_scripts_root', 'My Scripts', 'folder', NULL, 'global', 1, 'system', 'System', NOW(), NOW()),
-- ('shared_scripts_root', 'Shared Scripts', 'folder', NULL, 'global', 2, 'system', 'System', NOW(), NOW());
--
-- -- 创建索引优化查询性能
-- CREATE INDEX `idx_directory_tree_query` ON `mc_directory_node`(`service_name`, `parent_id`, `type`, `is_deleted`, `sort_order`);