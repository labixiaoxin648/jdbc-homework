-- ============================================================
--  ORM / JDBC 作业数据库脚本
--  数据库：DateTest
--  表：student（学生表）、college（学院表）
--  账号：root / 123456
-- ============================================================

DROP DATABASE IF EXISTS `DateTest`;
CREATE DATABASE `DateTest` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `DateTest`;

-- ------------------------------------------------------------
-- 学院表
-- ------------------------------------------------------------
CREATE TABLE `college` (
    `id`   INT         NOT NULL AUTO_INCREMENT COMMENT '学院主键',
    `name` VARCHAR(50) NOT NULL                COMMENT '学院名称',
    `code` VARCHAR(20) NOT NULL                COMMENT '学院代码',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学院表';

-- ------------------------------------------------------------
-- 学生表
-- 入学时间 -> enrollment_time，是否毕业 -> graduated，学费 -> tuition
-- ------------------------------------------------------------
CREATE TABLE `student` (
    `id`              INT            NOT NULL AUTO_INCREMENT COMMENT '学生主键',
    `name`            VARCHAR(50)    NOT NULL                COMMENT '姓名',
    `major`           VARCHAR(50)    DEFAULT NULL            COMMENT '专业',
    `age`             INT            DEFAULT NULL            COMMENT '年龄',
    `enrollment_time` DATE           DEFAULT NULL            COMMENT '入学时间',
    `graduated`       TINYINT(1)     NOT NULL DEFAULT 0      COMMENT '是否毕业：0 未毕业，1 已毕业',
    `tuition`         DECIMAL(10, 2) DEFAULT NULL            COMMENT '学费',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '学生表';

-- ------------------------------------------------------------
-- 初始数据
-- ------------------------------------------------------------
INSERT INTO `college` (`name`, `code`) VALUES
    ('计算机学院', 'CS'),
    ('外国语学院', 'FL'),
    ('数学学院', 'MATH');

INSERT INTO `student` (`name`, `major`, `age`, `enrollment_time`, `graduated`, `tuition`) VALUES
    ('张三', '软件工程',   20, '2023-09-01', 0, 5800.00),
    ('李四', '计算机科学', 22, '2021-09-01', 1, 5500.00),
    ('王五', '英语',       21, '2022-09-01', 0, 5000.00);
