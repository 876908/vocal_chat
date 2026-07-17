-- VocalChat Database Schema
-- 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS vocal_chat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vocal_chat;

-- ============================================================
-- 1. 系统用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱（登录账号）',
  `password` VARCHAR(200) COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '昵称',
  `avatar_url` VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  `gender` TINYINT DEFAULT 0 COMMENT '0-未知 1-男 2-女',
  `email_status` VARCHAR(20) DEFAULT 'UNVERIFIED' COMMENT 'UNVERIFIED/VERIFIED',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LOCKED/DELETED',
  `login_fail_count` INT DEFAULT 0 COMMENT '连续登录失败次数',
  `locked_until` DATETIME COMMENT '锁定截止时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_email` (`email`),
  INDEX `idx_status` (`status`)
) COMMENT '系统用户表';
