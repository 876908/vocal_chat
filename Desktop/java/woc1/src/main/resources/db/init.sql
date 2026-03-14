CREATE DATABASE IF NOT EXISTS team_manage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE team_manage;


CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(30) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `role` INT NOT NULL COMMENT '1-管理员 2-普通成员 3-审核员',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '系统用户表';


CREATE TABLE IF NOT EXISTS `team_info` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '队伍ID',
  `team_name` VARCHAR(50) NOT NULL COMMENT '队伍名称',
  `team_desc` VARCHAR(200) COMMENT '队伍描述',
  `owner_id` BIGINT NOT NULL COMMENT '创建人ID',
  `audit_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '审核状态：PENDING-待审核 PASS-通过 REJECT-拒绝',
  `audit_remark` VARCHAR(200) COMMENT '审核备注',
  `auditor_id` BIGINT COMMENT '审核人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `audit_time` DATETIME COMMENT '审核时间',
  FOREIGN KEY (`owner_id`) REFERENCES `sys_user`(`id`),
  FOREIGN KEY (`auditor_id`) REFERENCES `sys_user`(`id`)
) COMMENT '队伍信息表';


INSERT INTO `sys_user` (`username`, `password`, `role`) VALUES
('admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 1),
('auditor', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 3),
('member', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 2);