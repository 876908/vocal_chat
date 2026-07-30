-- ============================================================
-- VocalChat 数据库初始化
-- 基于 DESIGN_GUIDE 第4节数据库设计
-- ============================================================

CREATE DATABASE IF NOT EXISTS vocal_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vocal_chat;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '用户ID (UUID)',
  `nick_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '昵称',
  `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱（登录账号）',
  `password` VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_email` (`email`)
) ENGINE=InnoDB COMMENT '用户表';

-- ============================================================
-- 2. AI 助手表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_assistant` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '助手ID (UUID)',
  `user_id` VARCHAR(36) NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '助手名称',
  `description` VARCHAR(500) DEFAULT '' COMMENT '助手简介',
  `assistant_character` TEXT NOT NULL COMMENT '角色设定 (System Prompt)',
  `knowledge_base_id` VARCHAR(36) DEFAULT NULL COMMENT '关联知识库ID（可选）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT 'AI助手配置表';

-- ============================================================
-- 3. 对话表
-- ============================================================
CREATE TABLE IF NOT EXISTS `dialogue` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '对话ID (UUID)',
  `ai_assistant_id` VARCHAR(36) NOT NULL COMMENT '所属助手ID',
  `contexts` JSON COMMENT '对话上下文（消息数组）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_ai_assistant_id` (`ai_assistant_id`)
) ENGINE=InnoDB COMMENT '对话上下文表';

-- ============================================================
-- 4. 知识库表
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_base` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '知识库ID (UUID)',
  `user_id` VARCHAR(36) NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
  `description` VARCHAR(500) DEFAULT '' COMMENT '知识库描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '状态：UPLOADING/UPLOADED/PROCESSING/COMPLETED/FAILED',
  `document_count` INT DEFAULT 0 COMMENT '文档数量',
  `chunk_count` INT DEFAULT 0 COMMENT '切片数量',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT '知识库元数据表';

-- ============================================================
-- 5. 知识库文件表
-- ============================================================
CREATE TABLE IF NOT EXISTS `knowledge_base_file` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '文件ID (UUID)',
  `knowledge_base_id` VARCHAR(36) NOT NULL COMMENT '所属知识库ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `storage_key` VARCHAR(500) NOT NULL COMMENT '对象存储路径/Key',
  `status` VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '状态：UPLOADING/UPLOADED/PROCESSING/COMPLETED/FAILED',
  `chunk_count` INT DEFAULT 0 COMMENT '切片数量',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_knowledge_base_id` (`knowledge_base_id`)
) ENGINE=InnoDB COMMENT '知识库文件表';

-- ============================================================
-- 6. 语音通话记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `voice_call_session` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '通话记录ID (UUID)',
  `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
  `ai_assistant_id` VARCHAR(36) NOT NULL COMMENT 'AI助手ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'INITIATED' COMMENT '状态：INITIATED/IN_PROGRESS/COMPLETED/FAILED',
  `duration_seconds` INT DEFAULT 0 COMMENT '通话时长（秒）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_ai_assistant_id` (`ai_assistant_id`)
) ENGINE=InnoDB COMMENT '语音通话记录表';

-- ============================================================
-- 7. Agent 工具调用审计日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `agent_tool_call_log` (
  `id` VARCHAR(36) PRIMARY KEY COMMENT '日志ID (UUID)',
  `user_id` VARCHAR(36) NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(36) DEFAULT NULL COMMENT '关联对话/通话Session ID',
  `tool_name` VARCHAR(100) NOT NULL COMMENT '工具名称',
  `input_params` JSON COMMENT '输入参数',
  `output_result` JSON COMMENT '输出结果',
  `duration_ms` BIGINT DEFAULT 0 COMMENT '执行耗时（毫秒）',
  `success` TINYINT(1) DEFAULT 0 COMMENT '是否成功',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_session_id` (`session_id`)
) ENGINE=InnoDB COMMENT 'Agent工具调用审计日志表';