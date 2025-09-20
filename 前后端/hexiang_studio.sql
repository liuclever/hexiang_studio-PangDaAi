/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80033 (8.0.33)
 Source Host           : localhost:3306
 Source Schema         : hexiang_studio

 Target Server Type    : MySQL
 Target Server Version : 80033 (8.0.33)
 File Encoding         : 65001

 Date: 18/09/2025 15:34:11
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for activity_reservation
-- ----------------------------
DROP TABLE IF EXISTS `activity_reservation`;
CREATE TABLE `activity_reservation`  (
  `reservation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `plan_id` bigint NOT NULL COMMENT '活动计划ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT 'reserved' COMMENT '预约状态：reserved-已预约, cancelled-已取消',
  `reservation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '取消时间',
  `remark` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`reservation_id`) USING BTREE,
  UNIQUE INDEX `uk_plan_student_reservation`(`plan_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `fk_reservation_student`(`student_id` ASC) USING BTREE,
  CONSTRAINT `fk_reservation_plan` FOREIGN KEY (`plan_id`) REFERENCES `attendance_plan` (`plan_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_reservation_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of activity_reservation
-- ----------------------------
INSERT INTO `activity_reservation` VALUES (1, 1000009, 63, 'reserved', '2025-08-04 00:24:31', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (2, 1000009, 64, 'reserved', '2025-08-04 00:24:31', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (3, 1000009, 1, 'reserved', '2025-08-04 00:24:31', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (4, 1000010, 63, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (5, 1000010, 1, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (6, 1000010, 64, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (7, 1000010, 65, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (8, 1000010, 66, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (9, 1000010, 67, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (10, 1000010, 68, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (11, 1000010, 69, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (12, 1000010, 70, 'reserved', '2025-08-04 00:40:08', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (13, 1000016, 1, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (14, 1000016, 63, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (15, 1000016, 64, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (16, 1000016, 65, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (17, 1000016, 66, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (18, 1000016, 67, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (19, 1000016, 68, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (20, 1000016, 69, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (21, 1000016, 70, 'reserved', '2025-08-04 01:14:42', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (22, 1000017, 1, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (23, 1000017, 63, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (24, 1000017, 64, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (25, 1000017, 65, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (26, 1000017, 66, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (27, 1000017, 67, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (28, 1000017, 68, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (29, 1000017, 69, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (30, 1000017, 70, 'reserved', '2025-08-04 01:16:46', NULL, '系统自动创建预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (55, 1000021, 66, 'reserved', '2025-09-06 01:04:46', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (56, 1000021, 1, 'reserved', '2025-09-06 01:04:46', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (57, 1000021, 65, 'reserved', '2025-09-06 01:04:46', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (58, 1000021, 63, 'reserved', '2025-09-06 01:04:46', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (59, 1000021, 64, 'reserved', '2025-09-06 01:04:46', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (60, 1000022, 64, 'reserved', '2025-09-06 01:05:00', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (61, 1000022, 63, 'reserved', '2025-09-06 01:05:00', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (62, 1000023, 63, 'reserved', '2025-09-06 01:06:37', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (63, 1000023, 66, 'reserved', '2025-09-06 01:06:37', NULL, '创建活动时批量预约', NULL, NULL);
INSERT INTO `activity_reservation` VALUES (64, 1000023, 65, 'reserved', '2025-09-06 01:06:37', NULL, '创建活动时批量预约', NULL, NULL);

-- ----------------------------
-- Table structure for ai_conversation_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_conversation_log`;
CREATE TABLE `ai_conversation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `user_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户消息内容',
  `ai_response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI回复内容',
  `model_used` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '使用的AI模型',
  `question_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '问题分类',
  `response_time` int NULL DEFAULT 0 COMMENT '响应时间(毫秒)',
  `token_count` int NULL DEFAULT 0 COMMENT 'Token消耗数量',
  `success` tinyint(1) NULL DEFAULT 1 COMMENT '是否成功',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_session_time`(`user_id` ASC, `session_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_model_time`(`model_used` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_question_type_time`(`question_type` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI对话日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_conversation_log
-- ----------------------------

-- ----------------------------
-- Table structure for ai_model_usage_stats
-- ----------------------------
DROP TABLE IF EXISTS `ai_model_usage_stats`;
CREATE TABLE `ai_model_usage_stats`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `model_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模型名称',
  `question_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题类型',
  `usage_count` int NULL DEFAULT 1 COMMENT '使用次数',
  `total_turns` int NULL DEFAULT 1 COMMENT '总轮次',
  `success_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '成功率',
  `avg_response_time` decimal(8, 2) NULL DEFAULT 0.00 COMMENT '平均响应时间(ms)',
  `date_created` date NOT NULL COMMENT '统计日期',
  `last_updated` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_model_type_date`(`model_name` ASC, `question_type` ASC, `date_created` ASC) USING BTREE,
  INDEX `idx_model_name`(`model_name` ASC) USING BTREE,
  INDEX `idx_question_type`(`question_type` ASC) USING BTREE,
  INDEX `idx_date_created`(`date_created` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI模型使用统计表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_model_usage_stats
-- ----------------------------
INSERT INTO `ai_model_usage_stats` VALUES (1, 'qwen-plus', 'CASUAL', 10, 15, 0.00, 0.00, '2025-09-18', '2025-09-18 14:56:21');
INSERT INTO `ai_model_usage_stats` VALUES (2, 'qwen-max', 'COMPLEX_ANALYSIS', 5, 12, 0.00, 0.00, '2025-09-18', '2025-09-18 14:56:21');
INSERT INTO `ai_model_usage_stats` VALUES (3, 'qwen3-coder-plus-2025-07-22', 'CODE_GENERATION', 8, 20, 0.00, 0.00, '2025-09-18', '2025-09-18 14:56:21');
INSERT INTO `ai_model_usage_stats` VALUES (4, 'qwen-flash', 'CASUAL', 15, 18, 0.00, 0.00, '2025-09-18', '2025-09-18 14:56:21');
INSERT INTO `ai_model_usage_stats` VALUES (5, 'qwen-plus-latest', 'STUDIO_QUERY', 12, 25, 0.00, 0.00, '2025-09-18', '2025-09-18 14:56:21');

-- ----------------------------
-- Table structure for ai_session_state
-- ----------------------------
DROP TABLE IF EXISTS `ai_session_state`;
CREATE TABLE `ai_session_state`  (
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `current_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前使用的模型',
  `current_question_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '当前问题类型',
  `turn_count` int NULL DEFAULT 1 COMMENT '对话轮次',
  `model_locked` tinyint(1) NULL DEFAULT 0 COMMENT '模型是否锁定',
  `lock_reason` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '锁定原因',
  `recent_question_types` json NULL COMMENT '最近的问题类型历史',
  `first_interaction` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次交互时间',
  `last_interaction` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后交互时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`session_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_last_interaction`(`last_interaction` ASC) USING BTREE,
  INDEX `idx_current_model`(`current_model` ASC) USING BTREE,
  INDEX `idx_model_locked`(`model_locked` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI会话状态表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_session_state
-- ----------------------------

-- ----------------------------
-- Table structure for attendance_plan
-- ----------------------------
DROP TABLE IF EXISTS `attendance_plan`;
CREATE TABLE `attendance_plan`  (
  `plan_id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：activity-活动考勤,course-课程考勤,duty-值班考勤',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '考勤名称',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '地点',
  `location_lat` decimal(10, 6) NULL DEFAULT NULL COMMENT '纬度',
  `location_lng` decimal(10, 6) NULL DEFAULT NULL COMMENT '经度',
  `radius` int NOT NULL DEFAULT 100 COMMENT '签到有效半径(米)',
  `course_id` bigint NULL DEFAULT NULL COMMENT '关联课程ID(课程考勤专用)',
  `note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：1-有效，0-已取消',
  `create_user` bigint UNSIGNED NOT NULL COMMENT '创建人ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `schedule_id` bigint NULL DEFAULT NULL COMMENT '关联值班安排ID',
  `update_user` bigint NULL DEFAULT NULL,
  `processed` tinyint(1) NULL DEFAULT 0 COMMENT '是否已处理未签到记录',
  PRIMARY KEY (`plan_id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_time`(`start_time` ASC, `end_time` ASC) USING BTREE,
  INDEX `idx_course`(`course_id` ASC) USING BTREE,
  INDEX `idx_creator`(`create_user` ASC) USING BTREE,
  INDEX `fk_plan_schedule`(`schedule_id` ASC) USING BTREE,
  CONSTRAINT `fk_plan_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_plan_creator` FOREIGN KEY (`create_user`) REFERENCES `user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_plan_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `duty_schedule` (`schedule_id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1000024 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤计划表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance_plan
-- ----------------------------
INSERT INTO `attendance_plan` VALUES (9220, 'activity', '模拟活动考勤1', '2025-08-03 00:42:00', '2025-08-04 01:42:00', '办公室', 29.552424, 106.238485, 100, NULL, '备注1111', 1, 1, '2025-08-02 00:42:50', '2025-08-03 22:59:41', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (9221, 'activity', '模拟活动考勤2', '2025-08-04 00:42:00', '2025-08-05 01:42:00', '', 29.552424, 106.238485, 100, NULL, '备注1111', 1, 1, '2025-08-02 00:42:50', '2025-08-03 15:39:44', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (9224, 'course', '123', '2025-07-01 00:00:00', '2025-07-18 00:00:00', '哪里', 29.552500, 106.237800, 50, 12, '123', 1, 1, '2025-07-17 12:53:30', '2025-08-02 00:50:59', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (9227, 'activity', '哈哈哈哈', '2025-08-02 00:42:00', '2025-08-02 01:42:00', '', 29.552424, 106.238485, 100, NULL, '备注1111', 1, 1, '2025-08-02 00:42:50', '2025-08-03 01:23:12', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (9228, 'course', '课程考勤1', '2025-08-03 01:44:00', '2025-08-04 02:44:00', '办公室', 29.552991, 106.237566, 100, 20, '备注1', 1, 1, '2025-08-02 01:45:20', '2025-08-02 18:06:25', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000000, 'duty', '08:30-10:00 值班 - 考勤', '2025-07-29 08:30:00', '2025-07-29 10:00:00', '', 0.000000, 0.000000, 50, NULL, '系统自动创建的值班考勤计划', 1, 1, '2025-08-02 14:35:44', '2025-08-02 15:29:32', 888889, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000001, 'duty', '08:30-10:00 值班 - 考勤', '2025-08-05 08:30:00', '2025-08-05 10:00:00', '工作室', 0.000000, 0.000000, 50, NULL, '系统自动创建的值班考勤计划', 1, 1, '2025-08-03 01:22:49', '2025-08-03 01:22:49', 888890, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000002, 'duty', '08:30-10:00 值班 - 考勤', '2025-08-12 08:30:00', '2025-08-12 10:00:00', '工作室', 0.000000, 0.000000, 50, NULL, '系统自动创建的值班考勤计划', 1, 1, '2025-08-03 01:23:47', '2025-08-03 01:23:47', 888891, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000003, 'activity', '123', '2025-08-03 23:00:23', '2025-08-14 00:00:00', '学生食堂', 29.552500, 106.237800, 10, NULL, '123123', 1, 1, '2025-08-03 23:00:48', '2025-08-03 23:00:48', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000004, 'course', '课程考勤测试管理员创造', '2025-08-03 23:07:00', '2025-08-04 00:07:00', '办公室', 29.552991, 106.237566, 10, 1, '没有说明', 1, 1, '2025-08-03 23:08:07', '2025-08-03 23:08:07', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000005, 'activity', '123123', '2025-08-03 23:32:00', '2025-08-04 00:32:00', '办公室', 29.552991, 106.237566, 100, NULL, '123', 1, 1, '2025-08-03 23:35:21', '2025-08-03 23:35:21', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000006, 'activity', '活动考勤1', '2025-08-03 23:51:00', '2025-08-04 00:51:00', '办公室', 29.552991, 106.237566, 100, NULL, '123123', 1, 1, '2025-08-04 00:11:56', '2025-08-04 00:11:56', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000007, 'activity', '23131', '2025-08-04 00:15:00', '2025-08-04 01:15:00', '办公室', 29.552991, 106.237566, 100, NULL, '1231', 1, 1, '2025-08-04 00:16:02', '2025-08-04 00:16:02', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000008, 'activity', '123', '2025-08-04 00:17:00', '2025-08-04 01:17:00', '办公室', 29.552991, 106.237566, 100, NULL, '123123', 1, 1, '2025-08-04 00:17:44', '2025-08-04 00:17:44', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000009, 'activity', '123123', '2025-08-06 00:00:00', '2025-09-06 01:00:00', '办公室', 29.552991, 106.237566, 100, NULL, '123123', 1, 1, '2025-08-04 00:24:31', '2025-08-04 00:24:31', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000010, 'activity', 'hdkq', '2025-08-04 06:00:00', '2025-08-05 01:30:00', '办公室', 29.552991, 106.237566, 100, NULL, '12312', 1, 1, '2025-08-04 00:40:08', '2025-08-04 00:40:08', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000011, 'activity', 'vue考勤', '2025-08-04 02:00:37', '2025-08-06 00:00:00', '图书馆', 29.552424, 106.238485, 50, NULL, '1231231231', 1, 1, '2025-08-04 01:00:57', '2025-08-04 01:00:57', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000012, 'activity', 'vue3', '2025-08-04 02:03:09', '2025-08-05 00:00:00', 'E425', 29.553283, 106.236885, 50, NULL, '12313', 1, 1, '2025-08-04 01:02:37', '2025-08-04 01:02:37', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000013, 'activity', '12313', '2025-08-04 01:14:44', '2025-08-05 00:00:00', 'E425', 29.553283, 106.236885, 50, NULL, '1232131', 1, 1, '2025-08-04 01:10:04', '2025-08-04 01:10:04', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000014, 'activity', '123123', '2025-08-04 01:14:17', '2025-08-05 00:00:00', '图书馆', 29.552424, 106.238485, 50, NULL, '123123', 1, 1, '2025-08-04 01:10:38', '2025-08-04 01:10:38', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000015, 'activity', '123', '2025-08-04 01:44:06', '2025-08-05 00:00:00', '图书馆', 29.552424, 106.238485, 50, NULL, '123123', 1, 1, '2025-08-04 01:11:48', '2025-08-04 01:11:48', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000016, 'activity', '12', '2025-08-04 01:30:13', '2025-08-05 00:00:00', '图书馆', 29.552424, 106.238485, 50, NULL, '123123', 1, 1, '2025-08-04 01:14:42', '2025-08-04 01:14:42', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000017, 'activity', '123', '2025-08-05 00:00:00', '2025-08-06 00:00:00', 'E425', 29.553283, 106.236885, 50, NULL, '123123', 1, 1, '2025-08-04 01:16:46', '2025-08-04 01:16:46', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000021, 'activity', '123123', '2025-09-06 02:00:00', '2025-09-06 03:30:00', '办公室', 29.552991, 106.237566, 100, NULL, '13', 1, 2282, '2025-09-06 01:04:46', '2025-09-06 01:04:46', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000022, 'activity', '123213', '2025-09-06 02:00:00', '2025-09-06 02:04:00', '办公室', 29.552991, 106.237566, 100, NULL, '', 1, 2282, '2025-09-06 01:05:00', '2025-09-06 01:05:00', NULL, NULL, 0);
INSERT INTO `attendance_plan` VALUES (1000023, 'activity', '123123', '2025-09-06 01:30:00', '2025-09-06 02:06:00', '办公室', 29.552991, 106.237566, 100, NULL, '', 1, 2282, '2025-09-06 01:06:37', '2025-09-06 01:06:37', NULL, NULL, 0);

-- ----------------------------
-- Table structure for attendance_record
-- ----------------------------
DROP TABLE IF EXISTS `attendance_record`;
CREATE TABLE `attendance_record`  (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL COMMENT '考勤计划ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'absent' COMMENT '签到状态：present-已签到,late-迟到,absent-缺勤,leave-请假pending待签到',
  `sign_in_time` datetime NULL DEFAULT NULL COMMENT '签到时间',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '签到位置',
  `location_lat` decimal(10, 6) NULL DEFAULT NULL COMMENT '签到纬度',
  `location_lng` decimal(10, 6) NULL DEFAULT NULL COMMENT '签到经度',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_user` bigint UNSIGNED NULL DEFAULT NULL COMMENT '最后修改人ID',
  PRIMARY KEY (`record_id`) USING BTREE,
  UNIQUE INDEX `uk_plan_student`(`plan_id` ASC, `student_id` ASC) USING BTREE,
  INDEX `idx_student`(`student_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  CONSTRAINT `fk_record_plan` FOREIGN KEY (`plan_id`) REFERENCES `attendance_plan` (`plan_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_record_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4444 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance_record
-- ----------------------------
INSERT INTO `attendance_record` VALUES (4390, 9227, 1, 'present', '2025-08-02 00:43:55', '模拟位置', 29.552407, 106.238728, NULL, '2025-08-02 00:43:55', '2025-08-02 00:43:55', NULL);
INSERT INTO `attendance_record` VALUES (4391, 9228, 63, 'late', '2025-08-02 02:38:04', '模拟位置', 29.552887, 106.237317, NULL, '2025-08-02 02:38:04', '2025-08-02 02:38:04', NULL);
INSERT INTO `attendance_record` VALUES (4428, 1000000, 63, 'late', '2025-07-29 08:45:00', '工作室门口', 29.553017, 106.237538, '迟到15分钟', '2025-08-02 14:44:55', '2025-08-02 14:44:55', NULL);
INSERT INTO `attendance_record` VALUES (4429, 1000000, 1, 'present', '2025-07-29 08:35:00', '工作室', 29.553017, 106.237538, '准时到达', '2025-08-02 14:44:55', '2025-08-02 14:44:55', NULL);
INSERT INTO `attendance_record` VALUES (4430, 1000000, 64, 'absent', NULL, '', 0.000000, 0.000000, '未签到', '2025-08-02 14:44:55', '2025-08-02 14:44:55', NULL);
INSERT INTO `attendance_record` VALUES (4431, 1000000, 65, 'leave', NULL, '', 0.000000, 0.000000, '事假', '2025-08-02 14:44:55', '2025-08-02 14:44:55', NULL);
INSERT INTO `attendance_record` VALUES (4432, 1000000, 66, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-02 14:44:55', '2025-08-02 14:44:55', NULL);
INSERT INTO `attendance_record` VALUES (4433, 9227, 63, 'leave', NULL, '请假', NULL, NULL, '请假申请审批通过', '2025-08-02 22:45:46', '2025-08-02 22:45:46', 1);
INSERT INTO `attendance_record` VALUES (4434, 1000001, 63, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:22:49', '2025-08-03 01:22:49', NULL);
INSERT INTO `attendance_record` VALUES (4435, 1000001, 1, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:22:49', '2025-08-03 01:22:49', NULL);
INSERT INTO `attendance_record` VALUES (4436, 1000001, 64, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:22:49', '2025-08-03 01:22:49', NULL);
INSERT INTO `attendance_record` VALUES (4437, 1000001, 65, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:22:49', '2025-08-03 01:22:49', NULL);
INSERT INTO `attendance_record` VALUES (4438, 1000001, 66, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:22:49', '2025-08-03 01:22:49', NULL);
INSERT INTO `attendance_record` VALUES (4439, 1000002, 63, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:23:47', '2025-08-03 01:23:47', NULL);
INSERT INTO `attendance_record` VALUES (4440, 1000002, 1, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:23:47', '2025-08-03 01:23:47', NULL);
INSERT INTO `attendance_record` VALUES (4441, 1000002, 64, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:23:47', '2025-08-03 01:23:47', NULL);
INSERT INTO `attendance_record` VALUES (4442, 1000002, 65, 'pending', NULL, '', 0.000000, 0.000000, '', '2025-08-03 01:23:47', '2025-08-03 01:23:47', NULL);

-- ----------------------------
-- Table structure for attendance_statistics
-- ----------------------------
DROP TABLE IF EXISTS `attendance_statistics`;
CREATE TABLE `attendance_statistics`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型：activity-活动考勤,course-课程考勤,duty-值班考勤',
  `date` date NOT NULL COMMENT '统计日期',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总人数',
  `present_count` int NOT NULL DEFAULT 0 COMMENT '出勤人数',
  `late_count` int NOT NULL DEFAULT 0 COMMENT '迟到人数',
  `absent_count` int NOT NULL DEFAULT 0 COMMENT '缺勤人数',
  `leave_count` int NOT NULL DEFAULT 0 COMMENT '请假人数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_type_date`(`type` ASC, `date` ASC) USING BTREE COMMENT '确保每种类型每天只有一条记录'
) ENGINE = InnoDB AUTO_INCREMENT = 108 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤统计数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of attendance_statistics
-- ----------------------------
INSERT INTO `attendance_statistics` VALUES (49, 'course', '2025-07-01', 0, 0, 0, 0, 0, '2025-07-21 17:35:00', '2025-09-18 15:15:00');
INSERT INTO `attendance_statistics` VALUES (50, 'course', '2025-07-22', 0, 0, 0, 0, 0, '2025-07-23 08:43:12', '2025-07-23 08:43:12');
INSERT INTO `attendance_statistics` VALUES (51, 'activity', '2025-07-22', 0, 0, 0, 0, 0, '2025-07-23 08:43:12', '2025-07-23 08:43:12');
INSERT INTO `attendance_statistics` VALUES (52, 'duty', '2025-07-22', 0, 0, 0, 0, 0, '2025-07-23 08:43:12', '2025-07-23 08:43:12');
INSERT INTO `attendance_statistics` VALUES (53, 'course', '2025-07-24', 0, 0, 0, 0, 0, '2025-07-25 02:00:00', '2025-07-25 02:00:00');
INSERT INTO `attendance_statistics` VALUES (54, 'activity', '2025-07-24', 0, 0, 0, 0, 0, '2025-07-25 02:00:00', '2025-07-25 02:00:00');
INSERT INTO `attendance_statistics` VALUES (55, 'duty', '2025-07-24', 0, 0, 0, 0, 0, '2025-07-25 02:00:00', '2025-07-25 02:00:00');
INSERT INTO `attendance_statistics` VALUES (56, 'course', '2025-07-25', 0, 0, 0, 0, 0, '2025-07-26 02:00:00', '2025-07-26 02:00:00');
INSERT INTO `attendance_statistics` VALUES (57, 'activity', '2025-07-25', 0, 0, 0, 0, 0, '2025-07-26 02:00:00', '2025-07-26 02:00:00');
INSERT INTO `attendance_statistics` VALUES (58, 'duty', '2025-07-25', 0, 0, 0, 0, 0, '2025-07-26 02:00:00', '2025-07-26 02:00:00');
INSERT INTO `attendance_statistics` VALUES (59, 'course', '2025-07-26', 0, 0, 0, 0, 0, '2025-07-27 02:00:00', '2025-07-27 02:00:00');
INSERT INTO `attendance_statistics` VALUES (60, 'activity', '2025-07-26', 0, 0, 0, 0, 0, '2025-07-27 02:00:00', '2025-07-27 02:00:00');
INSERT INTO `attendance_statistics` VALUES (61, 'duty', '2025-07-26', 0, 0, 0, 0, 0, '2025-07-27 02:00:00', '2025-07-27 02:00:00');
INSERT INTO `attendance_statistics` VALUES (62, 'course', '2025-07-27', 0, 0, 0, 0, 0, '2025-07-28 02:00:00', '2025-07-28 02:00:00');
INSERT INTO `attendance_statistics` VALUES (63, 'activity', '2025-07-27', 0, 0, 0, 0, 0, '2025-07-28 02:00:00', '2025-07-28 02:00:00');
INSERT INTO `attendance_statistics` VALUES (64, 'duty', '2025-07-27', 0, 0, 0, 0, 0, '2025-07-28 02:00:00', '2025-07-28 02:00:00');
INSERT INTO `attendance_statistics` VALUES (65, 'course', '2025-07-28', 0, 0, 0, 0, 0, '2025-07-29 02:00:00', '2025-07-29 02:00:00');
INSERT INTO `attendance_statistics` VALUES (66, 'activity', '2025-07-28', 0, 0, 0, 0, 0, '2025-07-29 02:00:00', '2025-07-29 02:00:00');
INSERT INTO `attendance_statistics` VALUES (67, 'duty', '2025-07-28', 0, 0, 0, 0, 0, '2025-07-29 02:00:00', '2025-07-29 02:00:00');
INSERT INTO `attendance_statistics` VALUES (68, 'duty', '2025-07-29', 0, 0, 0, 0, 0, '2025-07-29 15:05:00', '2025-07-30 02:00:00');
INSERT INTO `attendance_statistics` VALUES (69, 'course', '2025-07-29', 0, 0, 0, 0, 0, '2025-07-30 02:00:00', '2025-07-30 02:00:00');
INSERT INTO `attendance_statistics` VALUES (70, 'activity', '2025-07-29', 0, 0, 0, 0, 0, '2025-07-30 02:00:00', '2025-07-30 02:00:00');
INSERT INTO `attendance_statistics` VALUES (71, 'course', '2025-07-30', 0, 0, 0, 0, 0, '2025-07-31 02:00:00', '2025-07-31 02:00:00');
INSERT INTO `attendance_statistics` VALUES (72, 'activity', '2025-07-30', 0, 0, 0, 0, 0, '2025-07-31 02:00:00', '2025-07-31 02:00:00');
INSERT INTO `attendance_statistics` VALUES (73, 'duty', '2025-07-30', 0, 0, 0, 0, 0, '2025-07-31 02:00:00', '2025-07-31 02:00:00');
INSERT INTO `attendance_statistics` VALUES (74, 'course', '2025-07-31', 0, 0, 0, 0, 0, '2025-08-01 15:28:46', '2025-08-01 15:28:46');
INSERT INTO `attendance_statistics` VALUES (75, 'activity', '2025-07-31', 0, 0, 0, 0, 0, '2025-08-01 15:28:46', '2025-08-01 15:28:46');
INSERT INTO `attendance_statistics` VALUES (76, 'duty', '2025-07-31', 0, 0, 0, 0, 0, '2025-08-01 15:28:46', '2025-08-01 15:28:46');
INSERT INTO `attendance_statistics` VALUES (77, 'course', '2025-08-01', 0, 0, 0, 0, 0, '2025-08-02 02:00:00', '2025-08-02 02:00:00');
INSERT INTO `attendance_statistics` VALUES (78, 'activity', '2025-08-01', 0, 0, 0, 0, 0, '2025-08-02 02:00:00', '2025-08-02 02:00:00');
INSERT INTO `attendance_statistics` VALUES (79, 'duty', '2025-08-01', 0, 0, 0, 0, 0, '2025-08-02 02:00:00', '2025-08-02 02:00:00');
INSERT INTO `attendance_statistics` VALUES (80, 'course', '2025-08-02', 0, 0, 0, 0, 0, '2025-08-02 02:45:00', '2025-08-03 02:00:00');
INSERT INTO `attendance_statistics` VALUES (81, 'activity', '2025-08-02', 2, 1, 0, 0, 1, '2025-08-02 22:45:46', '2025-08-03 02:00:00');
INSERT INTO `attendance_statistics` VALUES (82, 'duty', '2025-08-02', 0, 0, 0, 0, 0, '2025-08-03 02:00:00', '2025-08-03 02:00:00');
INSERT INTO `attendance_statistics` VALUES (83, 'course', '2025-08-03', 1, 0, 1, 0, 0, '2025-08-04 00:10:00', '2025-09-18 15:15:00');
INSERT INTO `attendance_statistics` VALUES (84, 'activity', '2025-08-03', 0, 0, 0, 0, 0, '2025-08-04 02:00:00', '2025-08-04 02:00:00');
INSERT INTO `attendance_statistics` VALUES (85, 'duty', '2025-08-03', 0, 0, 0, 0, 0, '2025-08-04 02:00:00', '2025-08-04 02:00:00');
INSERT INTO `attendance_statistics` VALUES (86, 'course', '2025-08-08', 0, 0, 0, 0, 0, '2025-08-09 00:26:33', '2025-08-09 00:26:33');
INSERT INTO `attendance_statistics` VALUES (87, 'activity', '2025-08-08', 0, 0, 0, 0, 0, '2025-08-09 00:26:33', '2025-08-09 00:26:33');
INSERT INTO `attendance_statistics` VALUES (88, 'duty', '2025-08-08', 0, 0, 0, 0, 0, '2025-08-09 00:26:33', '2025-08-09 00:26:33');
INSERT INTO `attendance_statistics` VALUES (89, 'course', '2025-08-10', 1, 1, 0, 0, 0, '2025-08-16 15:40:00', '2025-08-24 00:15:00');
INSERT INTO `attendance_statistics` VALUES (90, 'course', '2025-09-10', 0, 0, 0, 0, 0, '2025-09-11 04:14:13', '2025-09-11 04:14:13');
INSERT INTO `attendance_statistics` VALUES (91, 'activity', '2025-09-10', 0, 0, 0, 0, 0, '2025-09-11 04:14:13', '2025-09-11 04:14:13');
INSERT INTO `attendance_statistics` VALUES (92, 'duty', '2025-09-10', 0, 0, 0, 0, 0, '2025-09-11 04:14:13', '2025-09-11 04:14:13');
INSERT INTO `attendance_statistics` VALUES (93, 'course', '2025-09-11', 0, 0, 0, 0, 0, '2025-09-12 02:00:00', '2025-09-12 02:00:00');
INSERT INTO `attendance_statistics` VALUES (94, 'activity', '2025-09-11', 0, 0, 0, 0, 0, '2025-09-12 02:00:00', '2025-09-12 02:00:00');
INSERT INTO `attendance_statistics` VALUES (95, 'duty', '2025-09-11', 0, 0, 0, 0, 0, '2025-09-12 02:00:00', '2025-09-12 02:00:00');
INSERT INTO `attendance_statistics` VALUES (96, 'course', '2025-09-12', 0, 0, 0, 0, 0, '2025-09-13 02:00:00', '2025-09-13 02:00:00');
INSERT INTO `attendance_statistics` VALUES (97, 'activity', '2025-09-12', 0, 0, 0, 0, 0, '2025-09-13 02:00:00', '2025-09-13 02:00:00');
INSERT INTO `attendance_statistics` VALUES (98, 'duty', '2025-09-12', 0, 0, 0, 0, 0, '2025-09-13 02:00:00', '2025-09-13 02:00:00');
INSERT INTO `attendance_statistics` VALUES (99, 'course', '2025-09-14', 0, 0, 0, 0, 0, '2025-09-15 02:00:00', '2025-09-15 02:00:00');
INSERT INTO `attendance_statistics` VALUES (100, 'activity', '2025-09-14', 0, 0, 0, 0, 0, '2025-09-15 02:00:00', '2025-09-15 02:00:00');
INSERT INTO `attendance_statistics` VALUES (101, 'duty', '2025-09-14', 0, 0, 0, 0, 0, '2025-09-15 02:00:00', '2025-09-15 02:00:00');
INSERT INTO `attendance_statistics` VALUES (102, 'course', '2025-09-15', 0, 0, 0, 0, 0, '2025-09-16 02:00:00', '2025-09-16 02:00:00');
INSERT INTO `attendance_statistics` VALUES (103, 'activity', '2025-09-15', 0, 0, 0, 0, 0, '2025-09-16 02:00:00', '2025-09-16 02:00:00');
INSERT INTO `attendance_statistics` VALUES (104, 'duty', '2025-09-15', 0, 0, 0, 0, 0, '2025-09-16 02:00:00', '2025-09-16 02:00:00');
INSERT INTO `attendance_statistics` VALUES (105, 'course', '2025-09-16', 0, 0, 0, 0, 0, '2025-09-17 05:08:01', '2025-09-17 05:08:01');
INSERT INTO `attendance_statistics` VALUES (106, 'activity', '2025-09-16', 0, 0, 0, 0, 0, '2025-09-17 05:08:01', '2025-09-17 05:08:01');
INSERT INTO `attendance_statistics` VALUES (107, 'duty', '2025-09-16', 0, 0, 0, 0, 0, '2025-09-17 05:08:01', '2025-09-17 05:08:01');

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history`  (
  `chat_id` bigint NOT NULL AUTO_INCREMENT COMMENT '聊天记录ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `message_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型：user-用户消息，ai-AI回复',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `message_order` int NOT NULL DEFAULT 1 COMMENT '消息序号（在同一会话中的顺序）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`chat_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  CONSTRAINT `fk_chat_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 569 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI聊天历史记录表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_history
-- ----------------------------
INSERT INTO `chat_history` VALUES (561, 2283, '2283_stream_1758178648741', 'user', '当前用户询问：我要删除老李这个人', 1, '2025-09-18 14:57:35');
INSERT INTO `chat_history` VALUES (562, 2283, '2283_stream_1758178648741', 'ai', '我无法执行删除用户的操作。作为智能助手，我没有权限进行此类管理操作。建议您联系工作室的管理员或人事部门处理。', 2, '2025-09-18 14:57:35');
INSERT INTO `chat_history` VALUES (563, 2283, '2283_stream_1758178997832', 'user', '当前用户询问：我要删除老李这个人', 1, '2025-09-18 15:03:38');
INSERT INTO `chat_history` VALUES (564, 2283, '2283_stream_1758178997832', 'ai', '王刚，您好！很抱歉，您目前的角色是学员，没有权限执行删除用户的操作。用户管理权限仅限管理员和超级管理员。\n\n如果您有正当理由需要删除用户，请联系您的辅导员谭只或工作室管理员处理。', 2, '2025-09-18 15:03:38');
INSERT INTO `chat_history` VALUES (565, 1, '1_stream_1758179038913', 'user', '当前用户询问：我要删除老李这个人', 1, '2025-09-18 15:04:18');
INSERT INTO `chat_history` VALUES (566, 1, '1_stream_1758179038913', 'ai', '抱歉，当前会话未登录，无法执行删除操作。请先登录并确认权限后重试。', 2, '2025-09-18 15:04:18');
INSERT INTO `chat_history` VALUES (567, 1, '1_stream_1758179272939', 'user', '当前用户询问：我要删除老李这个人', 1, '2025-09-18 15:08:14');
INSERT INTO `chat_history` VALUES (568, 1, '1_stream_1758179272939', 'ai', '当前操作需要登录验证，请先登录以确认身份和权限，再进行删除用户操作。', 2, '2025-09-18 15:08:14');

-- ----------------------------
-- Table structure for common_location
-- ----------------------------
DROP TABLE IF EXISTS `common_location`;
CREATE TABLE `common_location`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '地点ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '地点名称',
  `latitude` double NOT NULL COMMENT '纬度',
  `longitude` double NOT NULL COMMENT '经度',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地点描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '常用签到地点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of common_location
-- ----------------------------
INSERT INTO `common_location` VALUES (1, '办公室', 29.552991350566856, 106.23756588726565, '教师办公室', '2025-06-15 19:44:46');
INSERT INTO `common_location` VALUES (2, 'E425', 29.553282731007233, 106.23688499773743, '教学楼E425教室', '2025-06-15 19:44:46');
INSERT INTO `common_location` VALUES (3, '图书馆', 29.552424399022044, 106.23848511371273, '学校图书馆', '2025-06-15 19:44:46');
INSERT INTO `common_location` VALUES (4, '学生食堂', 29.5525, 106.2378, '学生第一食堂', '2025-06-15 19:44:46');

-- ----------------------------
-- Table structure for course
-- ----------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course`  (
  `course_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL,
  `teacher_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `status` tinyint NULL DEFAULT 0 COMMENT '课程状态：0-草稿，1-已发布，2-已下架',
  `duration` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '课程时长，如\"24课时\"',
  `cover_image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '课程封面图片URL',
  `category_id` bigint NULL DEFAULT NULL COMMENT '课程分类ID（就是培训方向）',
  `location` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '上课地点',
  `schedule` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '上课时间',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_user` bigint NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`course_id`) USING BTREE,
  INDEX `fk_course_category`(`category_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course
-- ----------------------------
INSERT INTO `course` VALUES (1, '数学基础', '这是一门数学基础课程，涵盖数学的基本概念和运算。', 11120, '2025-06-01 09:00:00', 1, '23', 'sdasd', 4, 'E425', '每周6', NULL, NULL, NULL);
INSERT INTO `course` VALUES (4, '编程基础', '教授编程语言的基础知识和编程技巧。', 11118, '2025-06-04 12:00:00', 2, '22', '', 2, '123', '3123', NULL, NULL, NULL);
INSERT INTO `course` VALUES (5, '历史故事', '通过生动rettttttttttttttttttttt', 11119, '2025-06-04 12:00:00', 1, NULL, '234234https://www.doubao.com/chat/8622966299004930', NULL, '234', '234', 1, 1, '2025-06-13 01:54:04');
INSERT INTO `course` VALUES (12, '123', '12312123123123', NULL, NULL, 1, '123', '', 3, '123', '123213', NULL, NULL, NULL);
INSERT INTO `course` VALUES (13, '23432', '2342342342', NULL, NULL, 2, '23', '', 3, '234', '234', NULL, NULL, NULL);
INSERT INTO `course` VALUES (14, 'werw', '123122222222222', 11123, NULL, 1, '23', '', 4, 'E425', '123', NULL, NULL, NULL);
INSERT INTO `course` VALUES (15, '213', 'fghfhghkjhjk', 11123, '2025-07-08 18:14:53', 1, '23', '', 3, 'E425', 'fghfg', NULL, NULL, NULL);
INSERT INTO `course` VALUES (16, 'qwe', 'qweqweqweewq', 11123, NULL, 1, 'qwe', '', 3, 'E425', 'qweq', NULL, NULL, NULL);
INSERT INTO `course` VALUES (17, '123', '123123123123', 11123, NULL, 0, '123', NULL, 2, 'E425', '123', NULL, NULL, NULL);
INSERT INTO `course` VALUES (18, '123123', '12313123123123', 11123, NULL, 0, '123123', 'course/cover/2025/07/18/a145b257ebeb4b3c9c500d31cb9dfd39.png', 2, '办公室', '123', NULL, NULL, NULL);
INSERT INTO `course` VALUES (19, '123', '11111111111111111111111111', 11123, '2025-07-18 01:15:16', 1, '123', 'course/cover/2025/07/18/b44ece0fa0bb4de09c230a1d82328359.png', 3, 'E425', '123', NULL, 1, '2025-07-21 17:45:04');
INSERT INTO `course` VALUES (20, '123', '1231312313123', 11123, '2025-07-18 01:37:14', 2, '123', 'course/cover/2025/07/18/51615e090ed74a9681c25c8fe2199c81.png', 2, 'E425', '123', NULL, NULL, '2025-07-18 01:37:14');
INSERT INTO `course` VALUES (21, '123', '123123123123', 11124, '2025-08-04 01:37:19', 0, '123', 'course/cover/2025/08/04/47109c847fd94ee09ad1be013e0d068f.png', 2, 'E425', '123123132', 1, 2255, '2025-08-04 18:20:48');
INSERT INTO `course` VALUES (23, '测试课程1', '测试课程 仅用于测试', 11123, '2025-09-02 16:25:57', 1, '1课时', 'course/cover/2025/09/02/29abd51584dd47e788edecf259e0284d.jpg', 2, '办公室', '每周一 早上', 1, 1, '2025-09-02 16:25:58');
INSERT INTO `course` VALUES (24, '123', '12313123123123', 11123, '2025-09-10 00:47:02', 1, '123', 'course/cover/2025/09/10/a0761b37869e4fdcaa14b9a24db39332.jfif', 2, '办公室', '1231', 1, 1, '2025-09-10 00:47:02');

-- ----------------------------
-- Table structure for course_material
-- ----------------------------
DROP TABLE IF EXISTS `course_material`;
CREATE TABLE `course_material`  (
  `material_id` bigint NOT NULL AUTO_INCREMENT COMMENT '资料ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件原始名称',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件MIME类型',
  `download_count` int NULL DEFAULT 0 COMMENT '下载次数',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `uploader_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '上传者ID',
  PRIMARY KEY (`material_id`) USING BTREE,
  INDEX `idx_course_id`(`course_id` ASC) USING BTREE,
  INDEX `fk_material_uploader_course`(`uploader_id` ASC) USING BTREE,
  CONSTRAINT `fk_material_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`course_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_material_uploader_course` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程资料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_material
-- ----------------------------
INSERT INTO `course_material` VALUES (4, 20, '（网评）火眼警睛-内置“天宫智核”的校园深夜捍卫者 - 副本.docx', 'course/material/2025/07/18/bd9ef8a4a52a4a5998774ae90563f868.docx', 6741397, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 0, '2025-07-18 01:37:14', NULL);
INSERT INTO `course_material` VALUES (5, 20, '新建 Microsoft Excel 工作表.xlsx', 'course/material/2025/07/18/6f9c563ea1034e7fa458b163a6c4ecfd.xlsx', 8968, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 0, '2025-07-18 01:37:14', NULL);
INSERT INTO `course_material` VALUES (9, 23, '前端实现方案.docx', 'course/material/2025/09/02/65953e6ad7274a138ecaa087febd26d0.docx', 1588168, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 0, '2025-09-02 16:25:57', 1);
INSERT INTO `course_material` VALUES (10, 24, '头像.png', 'course/material/2025/09/10/257b5030fdba49cb930d4a8205d415b5.png', 201740, 'image/png', 0, '2025-09-10 00:47:02', 1);

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `department_id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `department_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '部门名称',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`department_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES (1, '运行部门', '2025-07-29 00:19:47');
INSERT INTO `department` VALUES (2, 'UI设计部', '2025-07-29 00:19:47');
INSERT INTO `department` VALUES (3, '项目管理部', '2025-07-29 00:19:47');
INSERT INTO `department` VALUES (4, '测试质量部', '2025-07-29 00:19:47');

-- ----------------------------
-- Table structure for duty_schedule
-- ----------------------------
DROP TABLE IF EXISTS `duty_schedule`;
CREATE TABLE `duty_schedule`  (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT,
  `duty_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '值班名称',
  `location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '值班地点',
  `time_slot` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '值班时间段，例如：08:30-10:00',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-有效，0-已取消',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`schedule_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 888892 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '值班安排表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of duty_schedule
-- ----------------------------
INSERT INTO `duty_schedule` VALUES (888889, '08:30-10:00 值班', '工作室', '08:30-10:00', '2025-07-29 08:30:00', '2025-07-29 10:00:00', 1, '2025-08-02 14:35:44', NULL);
INSERT INTO `duty_schedule` VALUES (888890, '08:30-10:00 值班', '工作室', '08:30-10:00', '2025-08-05 08:30:00', '2025-08-05 10:00:00', 1, '2025-08-03 01:22:49', NULL);
INSERT INTO `duty_schedule` VALUES (888891, '08:30-10:00 值班', '工作室', '08:30-10:00', '2025-08-12 08:30:00', '2025-08-12 10:00:00', 1, '2025-08-03 01:23:47', NULL);

-- ----------------------------
-- Table structure for duty_schedule_student
-- ----------------------------
DROP TABLE IF EXISTS `duty_schedule_student`;
CREATE TABLE `duty_schedule_student`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `schedule_id` bigint NOT NULL COMMENT '值班安排ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` bigint UNSIGNED NULL DEFAULT NULL COMMENT '创建人ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'normal',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_schedule_student`(`schedule_id` ASC, `student_id` ASC) USING BTREE COMMENT '确保一个值班安排中学生不重复',
  INDEX `idx_schedule_id`(`schedule_id` ASC) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  CONSTRAINT `fk_duty_student_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `duty_schedule` (`schedule_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2578 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '值班安排与学生关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of duty_schedule_student
-- ----------------------------
INSERT INTO `duty_schedule_student` VALUES (2563, 888889, 1, NULL, NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2564, 888889, 63, NULL, NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2565, 888889, 64, NULL, NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2566, 888889, 65, NULL, NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2567, 888889, 66, NULL, NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2568, 888890, 63, '2025-08-03 01:22:49', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2569, 888890, 1, '2025-08-03 01:22:49', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2570, 888890, 64, '2025-08-03 01:22:49', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2571, 888890, 65, '2025-08-03 01:22:49', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2572, 888890, 66, '2025-08-03 01:22:49', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2573, 888891, 63, '2025-08-03 01:23:47', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2574, 888891, 1, '2025-08-03 01:23:47', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2575, 888891, 64, '2025-08-03 01:23:47', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2576, 888891, 65, '2025-08-03 01:23:47', NULL, NULL);
INSERT INTO `duty_schedule_student` VALUES (2577, 888891, 66, '2025-08-03 01:23:47', NULL, NULL);

-- ----------------------------
-- Table structure for leave_request
-- ----------------------------
DROP TABLE IF EXISTS `leave_request`;
CREATE TABLE `leave_request`  (
  `request_id` bigint NOT NULL AUTO_INCREMENT COMMENT '请假申请ID',
  `student_id` bigint NOT NULL COMMENT '申请学生ID',
  `attendance_plan_id` bigint NULL DEFAULT NULL COMMENT '关联的考勤计划ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请假类型 (如: personal_leave-事假, sick_leave-病假)',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '请假事由',
  `start_time` datetime NOT NULL COMMENT '请假开始时间',
  `end_time` datetime NOT NULL COMMENT '请假结束时间',
  `attachments` json NULL COMMENT '附件URL列表 (JSON数组格式)',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'pending' COMMENT '状态: pending(待审批), approved(已通过), rejected(已驳回)',
  `approver_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '审批人ID',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审批备注 (例如驳回理由)',
  `approved_at` datetime NULL DEFAULT NULL COMMENT '审批操作时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`request_id`) USING BTREE,
  INDEX `idx_student_id`(`student_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `fk_leave_approver`(`approver_id` ASC) USING BTREE,
  INDEX `idx_attendance_plan`(`attendance_plan_id` ASC) USING BTREE,
  CONSTRAINT `fk_leave_approver` FOREIGN KEY (`approver_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_leave_attendance_plan` FOREIGN KEY (`attendance_plan_id`) REFERENCES `attendance_plan` (`plan_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_leave_student` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 116 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '请假申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of leave_request
-- ----------------------------
INSERT INTO `leave_request` VALUES (101, 1, NULL, 'sick_leave', '突发高烧，已在校医院就诊，需要卧床休息。', '2025-07-18 08:00:00', '2025-07-18 18:00:00', '[\"attachments/leave/sick_proof_zhangsan.jpg\"]', 'rejected', NULL, '123', '2025-07-17 16:51:29', '2025-07-17 20:10:00', '2025-07-17 16:51:28');
INSERT INTO `leave_request` VALUES (103, 63, NULL, 'personal_leave', '参加全国大学生编程竞赛，需要前往赛点城市。', '2025-07-20 00:00:00', '2025-07-22 23:59:59', '[\"attachments/leave/competition_invitation.pdf\", \"attachments/leave/travel_ticket.png\"]', 'approved', 1, '情况属实，预祝取得好成绩！', '2025-07-16 15:00:00', '2025-07-16 11:45:00', '2025-08-02 18:50:37');
INSERT INTO `leave_request` VALUES (105, 63, NULL, 'personal_leave', '国庆节提前回家，已购买车票。', '2025-09-30 18:00:00', '2025-10-07 23:59:59', NULL, 'rejected', 1, '根据学校规定，国庆假期前提早离校不予批准。', '2025-09-28 10:20:00', '2025-09-27 16:00:00', '2025-08-02 18:50:33');
INSERT INTO `leave_request` VALUES (106, 63, NULL, 'sick_leave', '123', '2025-07-29 08:30:00', '2025-07-29 10:00:00', '[\"leave/attachment/2025/08/02/883f2ad474c242d99f67aea892b1f039.jpg\"]', 'rejected', 1, '', '2025-08-02 18:58:44', '2025-08-02 18:57:57', '2025-08-02 18:58:52');
INSERT INTO `leave_request` VALUES (107, 63, NULL, 'sick_leave', '123123', '2025-07-29 08:30:00', '2025-07-29 10:00:00', '[\"leave/attachment/2025/08/02/4863ade030754f21a262687796553026.jpg\"]', 'rejected', 1, 'hhh', '2025-08-02 21:23:15', '2025-08-02 19:05:40', '2025-08-02 21:23:15');
INSERT INTO `leave_request` VALUES (108, 63, 1000000, 'sick_leave', '123', '2025-07-29 08:30:00', '2025-07-29 10:00:00', '[\"leave/attachment/2025/08/02/3bc0bf85582e42b4922c49aaad002ee7.jpg\"]', 'rejected', 1, '驳回理由', '2025-08-02 22:28:14', '2025-08-02 19:15:45', '2025-08-02 22:28:14');
INSERT INTO `leave_request` VALUES (109, 63, 1000000, 'public_leave', '请假原因', '2025-07-29 08:30:00', '2025-07-29 10:00:00', '[\"leave/attachment/2025/08/02/0d25f5f970974acb91fc6feb83f7406c.jpg\"]', 'rejected', 1, '这里是驳回原因', '2025-08-02 22:29:02', '2025-08-02 21:29:52', '2025-08-02 22:29:02');
INSERT INTO `leave_request` VALUES (110, 63, 1000000, 'personal_leave', '123123123', '2025-07-29 08:30:00', '2025-07-29 10:00:00', '[\"leave/attachment/2025/08/02/aa86d98fc3f44f909d42f6e5186b3734.jpg\"]', 'pending', NULL, NULL, NULL, '2025-08-02 22:15:02', '2025-08-02 22:15:02');
INSERT INTO `leave_request` VALUES (111, 63, 9227, 'personal_leave', '12312312', '2025-08-02 00:30:00', '2025-08-02 01:30:00', '[\"leave/attachment/2025/08/02/21769a217cf749bf844d40b91c9209bc.jpg\"]', 'approved', 1, NULL, '2025-08-02 22:45:46', '2025-08-02 22:18:42', '2025-08-02 22:45:46');
INSERT INTO `leave_request` VALUES (112, 63, 9228, 'public_leave', '1231', '2025-08-03 01:30:00', '2025-08-04 02:30:00', '[\"leave/attachment/2025/08/02/3590b113a9044943bbee063ad70effd2.jpg\"]', 'rejected', 1, '123', '2025-08-02 22:46:55', '2025-08-02 22:36:22', '2025-08-02 22:46:55');
INSERT INTO `leave_request` VALUES (113, 63, 9228, 'personal_leave', '原因11111', '2025-08-03 01:30:00', '2025-08-04 02:30:00', '[\"leave/attachment/2025/08/02/eba5e517d70b443385c8639a2c793e2e.jpg\"]', 'rejected', 1, '1233123', '2025-08-02 22:49:10', '2025-08-02 22:48:41', '2025-08-02 22:49:10');
INSERT INTO `leave_request` VALUES (114, 63, 9228, 'sick_leave', '123123', '2025-08-03 01:30:00', '2025-08-04 02:30:00', '[\"leave/attachment/2025/08/02/320113785970474abc917110f0b6cd77.jpg\"]', 'pending', NULL, NULL, NULL, '2025-08-02 22:50:19', '2025-08-02 22:50:19');
INSERT INTO `leave_request` VALUES (115, 63, 1000009, 'sick_leave', '哈哈哈吧', '2025-08-06 00:00:00', '2025-09-06 01:00:00', NULL, 'rejected', 1, '就很好', '2025-09-02 16:22:58', '2025-09-02 16:22:12', '2025-09-02 16:22:58');

-- ----------------------------
-- Table structure for manager
-- ----------------------------
DROP TABLE IF EXISTS `manager`;
CREATE TABLE `manager`  (
  `manager_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL,
  PRIMARY KEY (`manager_id`) USING BTREE,
  INDEX `mau`(`user_id` ASC) USING BTREE,
  CONSTRAINT `mau` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 31 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of manager
-- ----------------------------

-- ----------------------------
-- Table structure for material
-- ----------------------------
DROP TABLE IF EXISTS `material`;
CREATE TABLE `material`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '文件名',
  `file_type` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '文件类型/扩展名',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '资料访问路径',
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '资料描述',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类ID',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `uploader_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '上传者ID',
  `download_count` bigint NULL DEFAULT 0 COMMENT '下载次数',
  `is_public` tinyint(1) NULL DEFAULT 1 COMMENT '是否公开 0不公开 1公开',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，0-已删除',
  `update_time` datetime NULL DEFAULT NULL,
  `update_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_material_category`(`category_id` ASC) USING BTREE,
  INDEX `fk_material_uploader`(`uploader_id` ASC) USING BTREE,
  CONSTRAINT `fk_material_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 81 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '资料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material
-- ----------------------------
INSERT INTO `material` VALUES (78, '头像.png', 'png', 201740, 'material/分类1/2025/09/02/296ad97759c64272af5ed760b6942685.png', '123123123123', 52, '2025-09-02 16:31:59', 1, 1, 1, 1, '2025-09-02 16:31:59', 1);
INSERT INTO `material` VALUES (79, '头像.png', 'png', 201740, 'material/12313/2025/09/10/955b7b8a71d943aab2a43768dd6bae92.png', '123123', 44, '2025-09-10 00:49:49', 1, 2, 1, 1, '2025-09-10 00:49:49', 1);
INSERT INTO `material` VALUES (80, '课表.png', 'png', 1092189, 'material/12313/2025/09/13/c71b9ea570004577ad7899127168a383.png', '12313', 44, '2025-09-13 01:38:42', 1, 0, 1, 1, '2025-09-13 01:38:42', 1);

-- ----------------------------
-- Table structure for material_category
-- ----------------------------
DROP TABLE IF EXISTS `material_category`;
CREATE TABLE `material_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '分类名称',
  `order_id` bigint NOT NULL DEFAULT 0 COMMENT '排序权重',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`, `name`) USING BTREE,
  UNIQUE INDEX `name——uni`(`name` ASC) USING BTREE COMMENT '唯一',
  INDEX `id`(`id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 56 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '资料分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material_category
-- ----------------------------
INSERT INTO `material_category` VALUES (44, '12313', 9, NULL, NULL, NULL, NULL);
INSERT INTO `material_category` VALUES (48, '123123123', 1, '2025-07-16 17:09:15', '2025-07-16 17:09:15', '1', 1);
INSERT INTO `material_category` VALUES (49, '123123123123123', 1, '2025-07-16 17:09:58', '2025-07-16 17:09:58', '1', 1);
INSERT INTO `material_category` VALUES (50, '111111', 1, '2025-07-16 17:18:09', '2025-07-16 17:18:09', '1', 1);
INSERT INTO `material_category` VALUES (51, '1231', 4, '2025-08-03 01:45:41', '2025-08-03 01:45:41', '1', 1);
INSERT INTO `material_category` VALUES (52, '分类1', 1, '2025-08-03 01:49:40', '2025-08-03 01:49:40', '1', 1);
INSERT INTO `material_category` VALUES (53, '分类2', 1, '2025-08-03 01:51:22', '2025-08-03 01:51:22', '1', 1);
INSERT INTO `material_category` VALUES (54, '分类3', 1, '2025-08-03 01:51:28', '2025-08-03 01:51:28', '1', 1);

-- ----------------------------
-- Table structure for material_download_record
-- ----------------------------
DROP TABLE IF EXISTS `material_download_record`;
CREATE TABLE `material_download_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `material_id` bigint NOT NULL COMMENT '资料ID',
  `user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '下载用户ID',
  `download_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间',
  `ip_address` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '下载IP地址',
  `device_info` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备信息',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_download_material`(`material_id` ASC) USING BTREE,
  INDEX `fk_download_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_download_material` FOREIGN KEY (`material_id`) REFERENCES `material` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_download_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 70 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '资料下载记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of material_download_record
-- ----------------------------
INSERT INTO `material_download_record` VALUES (67, 78, 1, '2025-09-10 00:49:40', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0');
INSERT INTO `material_download_record` VALUES (68, 79, 1, '2025-09-10 00:49:51', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0');
INSERT INTO `material_download_record` VALUES (69, 79, 1, '2025-09-13 01:38:33', '127.0.0.1', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0');

-- ----------------------------
-- Table structure for model_selection_log
-- ----------------------------
DROP TABLE IF EXISTS `model_selection_log`;
CREATE TABLE `model_selection_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `message_length` int NOT NULL DEFAULT 0 COMMENT '消息长度',
  `question_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题类型：CASUAL/STUDIO_QUERY/STUDIO_MANAGEMENT/COMPLEX_ANALYSIS/LONG_CONTENT/CODE_GENERATION',
  `selected_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '选择的模型名称',
  `model_cost_rate` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '模型成本倍率',
  `context_complexity` int NULL DEFAULT 0 COMMENT '上下文复杂度',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `turn_count` int NULL DEFAULT 1 COMMENT '会话轮次',
  `model_locked` tinyint(1) NULL DEFAULT 0 COMMENT '模型是否被锁定',
  `previous_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上一个使用的模型',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_session`(`session_id` ASC) USING BTREE,
  INDEX `idx_model_time`(`selected_model` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI模型选择日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of model_selection_log
-- ----------------------------
INSERT INTO `model_selection_log` VALUES (1, 2283, '2283_stream_1758178648741', 16, 'CASUAL', 'qwen-plus', 2.00, 0, '2025-09-18 14:57:31', 2, 0, NULL);
INSERT INTO `model_selection_log` VALUES (2, 2283, '2283_stream_1758178997832', 16, 'CASUAL', 'qwen-plus', 2.00, 0, '2025-09-18 15:03:23', 2, 0, NULL);
INSERT INTO `model_selection_log` VALUES (3, 1, '1_stream_1758179038913', 16, 'CASUAL', 'qwen-plus', 2.00, 0, '2025-09-18 15:04:02', 2, 0, NULL);
INSERT INTO `model_selection_log` VALUES (4, 1, '1_stream_1758179132575', 16, 'CASUAL', 'qwen-plus', 2.00, 0, '2025-09-18 15:05:38', 2, 0, NULL);
INSERT INTO `model_selection_log` VALUES (5, 1, '1_stream_1758179272939', 16, 'CASUAL', 'qwen-plus', 2.00, 0, '2025-09-18 15:07:59', 2, 0, NULL);

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `noticeId` bigint NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '公告内容',
  `publishTime` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-已发布，0-草稿',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '公告类型：0通知 1活动 2新闻',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `update_user` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `publisher` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`noticeId`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 84 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (75, 'qwe', 'qwe', '2025-09-06 01:37:34', 1, 1, '2025-07-12 23:55:39', '2025-07-12 23:55:39', NULL, NULL, '管理员');
INSERT INTO `notice` VALUES (77, '123123', '121233', '2025-09-06 01:37:35', 1, 1, '2025-07-16 17:11:52', '2025-07-16 17:11:52', NULL, NULL, '管理员');
INSERT INTO `notice` VALUES (78, '123', '123123', '2025-09-10 00:49:32', 1, 2, '2025-07-17 17:29:14', '2025-07-17 17:29:14', NULL, NULL, 'hhh');
INSERT INTO `notice` VALUES (81, '华为培训', '此处为内容说明。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。', '2025-09-06 01:39:31', 1, 1, '2025-09-02 16:31:42', '2025-09-02 16:31:42', NULL, NULL, '管理员');
INSERT INTO `notice` VALUES (82, '123123', '12313213', '2025-09-06 01:38:52', 1, 1, '2025-09-06 01:38:51', '2025-09-06 01:38:51', NULL, NULL, '管理员');
INSERT INTO `notice` VALUES (83, '12313', '1231', '2025-09-06 01:39:20', 1, 1, '2025-09-06 01:39:20', '2025-09-06 01:39:20', NULL, NULL, '管理员');

-- ----------------------------
-- Table structure for notice_attachment
-- ----------------------------
DROP TABLE IF EXISTS `notice_attachment`;
CREATE TABLE `notice_attachment`  (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `file_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '文件名称',
  `file_path` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '文件路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件类型',
  `download_count` int NULL DEFAULT 0 COMMENT '下载次数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`attachment_id`) USING BTREE,
  INDEX `idx_notice_id`(`notice_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '公告附件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice_attachment
-- ----------------------------
INSERT INTO `notice_attachment` VALUES (40, 77, '（网评）火眼警睛-内置“天宫智核”的校园深夜捍卫者 - 副本(1).docx', 'notice/attachment/2025/07/16/5ac35816d7464c5191a2b485ddca4e04.docx', 9490568, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 0, NULL, NULL);
INSERT INTO `notice_attachment` VALUES (41, 78, '头像.png', 'notice/attachment/2025/07/17/56e1a43b82d24069bcd06e434a7a4ca6.png', 201740, 'image/png', 0, NULL, NULL);
INSERT INTO `notice_attachment` VALUES (45, 81, '第15期总天数统计表(1) 的副本.pdf', 'notice/attachment/2025/09/02/96bc3bcf472e4fa4a694bda9f880df8a.pdf', 960900, 'application/pdf', 0, NULL, NULL);

-- ----------------------------
-- Table structure for notice_image
-- ----------------------------
DROP TABLE IF EXISTS `notice_image`;
CREATE TABLE `notice_image`  (
  `image_id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `notice_id` bigint NOT NULL COMMENT '公告ID',
  `image_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '图片名称',
  `image_path` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '图片路径',
  `image_size` bigint NULL DEFAULT NULL COMMENT '图片大小(字节)',
  `image_type` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '图片类型(如image/jpeg)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`image_id`) USING BTREE,
  INDEX `idx_notice_id`(`notice_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 85 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '公告展示图片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notice_image
-- ----------------------------
INSERT INTO `notice_image` VALUES (75, 75, '新建 文本文档.txt', 'notice/image/2025/07/13/1a3af6b41f8b4d6d8c6cf95cfb3c1613.txt', 0, 'text/plain', NULL, NULL);
INSERT INTO `notice_image` VALUES (77, 77, '头像.png', 'notice/image/2025/07/16/79216940f2114eaab7ff24877b1eb0ad.png', 201740, 'image/png', NULL, NULL);
INSERT INTO `notice_image` VALUES (78, 78, '证件红底.jpg', 'notice/image/2025/07/17/f34d8db8218e4f1790efb0ae46d7493e.jpg', 92587, 'image/jpeg', NULL, NULL);
INSERT INTO `notice_image` VALUES (80, 78, '头像.png', 'notice/image/2025/07/21/2684fed628cf4cb3b0d9f805d33dc86a.png', 201740, 'image/png', NULL, NULL);
INSERT INTO `notice_image` VALUES (82, 81, '屏幕截图 2025-07-02 181037.png', 'notice/image/2025/09/02/8f56b85939f3404585a3eff2989d8d7d.png', 16586, 'image/png', NULL, NULL);
INSERT INTO `notice_image` VALUES (83, 82, '课表.png', 'notice/image/2025/09/06/5f3c34c8b3f1482386c071956a409634.png', 1092189, 'image/png', NULL, NULL);
INSERT INTO `notice_image` VALUES (84, 83, '证件红底.jpg', 'notice/image/2025/09/06/334219b641484badb51d52fe69fbe422.jpg', 92587, 'image/jpeg', NULL, NULL);

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission`  (
  `permission_id` bigint NOT NULL AUTO_INCREMENT,
  `permission_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `permission_code` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`permission_id`) USING BTREE,
  UNIQUE INDEX `permission_code`(`permission_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of permission
-- ----------------------------
INSERT INTO `permission` VALUES (1, '查看学生基本信息', 'student:view_basic', '学生查看自己的基本信息', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (2, '查看学生成绩', 'student:view_grade', '学生查看自己的成绩', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (3, '查看学生考勤', 'student:view_attend', '学生查看自己的考勤记录（不含统计）', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (4, '查看课程信息', 'course:view', '查看课程详情', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (5, '修改课程信息', 'course:edit', '修改自己负责的课程信息', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (6, '查看考勤统计', 'attend:view_stat', '查看考勤统计数据', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (7, '管理考勤记录', 'attend:manage', '新增、修改考勤记录', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (8, '管理学生信息', 'student:manage', '管理员管理学生信息', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (9, '管理教师信息', 'teacher:manage', '管理员管理教师信息', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (10, '管理课程信息', 'course:manage', '管理员管理所有课程', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (11, '发布公告', 'announcement:publish', '管理员发布公告', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (12, '查看公告', 'announcement:view', '所有用户查看公告', '2025-06-02 22:11:48', '2025-06-02 22:11:48');
INSERT INTO `permission` VALUES (13, '管理员访问权限', 'admin:access', '允许访问管理员路径', '2025-06-03 12:06:18', '2025-06-03 12:06:18');
INSERT INTO `permission` VALUES (14, '查看用户详情权限', 'user:view_basic', '查看用户详情资料', '2025-06-07 11:55:45', '2025-06-07 11:55:47');

-- ----------------------------
-- Table structure for position
-- ----------------------------
DROP TABLE IF EXISTS `position`;
CREATE TABLE `position`  (
  `position_id` smallint UNSIGNED NOT NULL AUTO_INCREMENT,
  `role` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '角色（对应 user.role）',
  `position_name` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '职位名称',
  `permissions` json NULL COMMENT '权限列表',
  PRIMARY KEY (`position_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 124 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of position
-- ----------------------------
INSERT INTO `position` VALUES (0, 'visitor', '访客', '[\"system:view\"]');
INSERT INTO `position` VALUES (1, 'student', '普通学员', '[\"course:view\"]');
INSERT INTO `position` VALUES (3, 'student', '部长', '[\"course:manage\"]');
INSERT INTO `position` VALUES (4, 'student', '副部长', '[\"course:manage\"]');
INSERT INTO `position` VALUES (5, 'teacher', '老师', '[\"course:edit\"]');
INSERT INTO `position` VALUES (6, 'manager', '主任', '[\"system:config\"]');
INSERT INTO `position` VALUES (7, 'manager', '副主任', '[\"system:config\"]');
INSERT INTO `position` VALUES (8, 'admin', '超级管理员', '[\"system:config\"]');

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role`  (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID，主键自增',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称，如管理员、用户等',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '0' COMMENT '角色级别，数字越小权限越高',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述信息',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role
-- ----------------------------
INSERT INTO `role` VALUES (0, '访客', '0', '最低');
INSERT INTO `role` VALUES (1, '学员', '0', NULL);
INSERT INTO `role` VALUES (2, '老师', '1', NULL);
INSERT INTO `role` VALUES (3, '管理员', '0', NULL);
INSERT INTO `role` VALUES (4, '超级管理员', '0', NULL);

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission`  (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `permission_id`) USING BTREE,
  INDEX `fk_role_permission_permission_idx`(`permission_id` ASC) USING BTREE,
  CONSTRAINT `fk_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `permission` (`permission_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of role_permission
-- ----------------------------

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `student_id` bigint NOT NULL AUTO_INCREMENT COMMENT '学生id',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户id',
  `grade_year` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '入学年份',
  `majorClass` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '专业',
  `student_number` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '学号',
  `direction_id` bigint NOT NULL COMMENT '培训方向',
  `counselor` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '辅导员',
  `dormitory` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '宿舍-楼号',
  `score` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '分数',
  `department_id` bigint NULL DEFAULT NULL COMMENT '所属部门ID',
  PRIMARY KEY (`student_id`) USING BTREE,
  INDEX `stuu`(`user_id` ASC) USING BTREE,
  INDEX `fk_student_department`(`department_id` ASC) USING BTREE,
  CONSTRAINT `fk_student_department` FOREIGN KEY (`department_id`) REFERENCES `department` (`department_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `stuu` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 74 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, 1, '2024', '软件工程2班', '3453454353', 3, '阿斯顿', '12栋', '', 1);
INSERT INTO `student` VALUES (63, 2271, '2022', '123', '12356789', 5, '123', '123', NULL, 2);
INSERT INTO `student` VALUES (64, 2272, '2022', '123123', '123123', 4, '123', '123', NULL, 3);
INSERT INTO `student` VALUES (65, 2273, '2022', '2023级计算机应用02班', '1234567', 2, '八九岁的', '12栋', NULL, 4);
INSERT INTO `student` VALUES (66, 2274, '2026', '2023级现代通信工程02班', '123456', 5, '赵孙', '16栋', NULL, NULL);
INSERT INTO `student` VALUES (67, 2275, '2022', '2023级现通信工程02班', '123123123', 3, '赵树海', '13栋', NULL, 1);
INSERT INTO `student` VALUES (68, 2276, '2022', '2023级现代通信工程02班', '1231', 4, '请问', '13栋', NULL, NULL);
INSERT INTO `student` VALUES (69, 2277, '2022', '2023级现代通信工程02班', '123123123', 5, '啊实打实', '12栋', NULL, NULL);
INSERT INTO `student` VALUES (70, 2278, '2026', '2023级现代通信工程02班', '123123123', 4, '侯丹', '23栋', NULL, 3);
INSERT INTO `student` VALUES (71, 2281, '2026', '2023级通信工程02班', '1231231231', 4, '啊飒飒的', '12栋', NULL, 3);
INSERT INTO `student` VALUES (72, 2283, '2026', '2023级现代通信工程02班', '123456', 5, '谭只', '12栋', NULL, 2);
INSERT INTO `student` VALUES (73, 2284, '2022', '2023级现代通信工程02班', '12313213123', 3, '撒吊袜带', '12栋', NULL, 2);

-- ----------------------------
-- Table structure for student_course
-- ----------------------------
DROP TABLE IF EXISTS `student_course`;
CREATE TABLE `student_course`  (
  `student_id` bigint NOT NULL,
  `course_id` bigint NOT NULL AUTO_INCREMENT,
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `create_user` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '添加人',
  INDEX `course_id`(`course_id` ASC) USING BTREE,
  INDEX `11`(`student_id` ASC) USING BTREE,
  CONSTRAINT `11` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student_course
-- ----------------------------
INSERT INTO `student_course` VALUES (1, 4, '2025-06-18 11:47:52', '2025-06-18 11:47:52', NULL);
INSERT INTO `student_course` VALUES (1, 1, '2025-06-24 09:35:45', '2025-06-24 09:35:45', NULL);
INSERT INTO `student_course` VALUES (1, 14, '2025-07-13 22:30:52', '2025-07-13 22:30:52', NULL);
INSERT INTO `student_course` VALUES (63, 20, '2025-08-02 16:00:24', '2025-08-02 16:00:23', NULL);
INSERT INTO `student_course` VALUES (67, 17, '2025-08-02 16:00:57', '2025-08-02 16:00:56', NULL);
INSERT INTO `student_course` VALUES (65, 17, '2025-08-02 16:00:58', '2025-08-02 16:00:57', NULL);
INSERT INTO `student_course` VALUES (64, 17, '2025-08-02 16:00:58', '2025-08-02 16:00:57', NULL);
INSERT INTO `student_course` VALUES (63, 17, '2025-08-02 16:00:58', '2025-08-02 16:00:58', NULL);
INSERT INTO `student_course` VALUES (67, 19, '2025-08-02 16:01:06', '2025-08-02 16:01:06', NULL);
INSERT INTO `student_course` VALUES (68, 19, '2025-08-02 16:01:07', '2025-08-02 16:01:07', NULL);
INSERT INTO `student_course` VALUES (70, 19, '2025-08-02 16:01:08', '2025-08-02 16:01:08', NULL);
INSERT INTO `student_course` VALUES (63, 18, '2025-08-02 16:13:47', '2025-08-02 16:13:47', NULL);
INSERT INTO `student_course` VALUES (64, 18, '2025-08-02 16:13:49', '2025-08-02 16:13:48', NULL);
INSERT INTO `student_course` VALUES (65, 18, '2025-08-02 16:13:49', '2025-08-02 16:13:49', NULL);
INSERT INTO `student_course` VALUES (67, 18, '2025-08-02 16:13:50', '2025-08-02 16:13:49', NULL);
INSERT INTO `student_course` VALUES (69, 18, '2025-08-02 16:13:50', '2025-08-02 16:13:50', NULL);
INSERT INTO `student_course` VALUES (64, 20, '2025-08-02 16:14:03', '2025-08-02 16:14:03', NULL);
INSERT INTO `student_course` VALUES (65, 20, '2025-08-02 16:14:04', '2025-08-02 16:14:03', NULL);
INSERT INTO `student_course` VALUES (67, 20, '2025-08-02 16:14:04', '2025-08-02 16:14:04', NULL);
INSERT INTO `student_course` VALUES (69, 20, '2025-08-02 16:14:05', '2025-08-02 16:14:05', NULL);
INSERT INTO `student_course` VALUES (63, 21, '2025-08-04 18:20:42', '2025-08-04 18:20:42', NULL);
INSERT INTO `student_course` VALUES (64, 21, '2025-08-04 18:20:43', '2025-08-04 18:20:43', NULL);
INSERT INTO `student_course` VALUES (63, 24, '2025-09-10 00:48:15', '2025-09-10 00:48:15', NULL);
INSERT INTO `student_course` VALUES (64, 24, '2025-09-10 00:48:16', '2025-09-10 00:48:15', NULL);
INSERT INTO `student_course` VALUES (65, 24, '2025-09-10 00:48:16', '2025-09-10 00:48:16', NULL);

-- ----------------------------
-- Table structure for student_direction
-- ----------------------------
DROP TABLE IF EXISTS `student_direction`;
CREATE TABLE `student_direction`  (
  `student_id` bigint NOT NULL COMMENT '学生ID（与student表一致）',
  `direction_id` bigint NOT NULL COMMENT '方向ID（与training_direction表一致）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选择时间',
  PRIMARY KEY (`student_id`, `direction_id`) USING BTREE,
  INDEX `direction_id`(`direction_id` ASC) USING BTREE,
  CONSTRAINT `student_direction_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`student_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `student_direction_ibfk_2` FOREIGN KEY (`direction_id`) REFERENCES `training_direction` (`direction_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student_direction
-- ----------------------------
INSERT INTO `student_direction` VALUES (1, 5, '2025-07-13 16:09:07');
INSERT INTO `student_direction` VALUES (63, 2, '2025-07-18 02:18:22');
INSERT INTO `student_direction` VALUES (63, 4, '2025-07-18 02:18:22');
INSERT INTO `student_direction` VALUES (63, 5, '2025-07-18 02:18:22');
INSERT INTO `student_direction` VALUES (64, 2, '2025-07-23 08:58:38');
INSERT INTO `student_direction` VALUES (65, 2, '2025-07-23 09:12:21');
INSERT INTO `student_direction` VALUES (66, 1, '2025-07-29 00:56:42');
INSERT INTO `student_direction` VALUES (66, 4, '2025-07-29 00:56:42');
INSERT INTO `student_direction` VALUES (66, 5, '2025-07-29 00:56:42');
INSERT INTO `student_direction` VALUES (67, 2, '2025-07-29 01:08:36');
INSERT INTO `student_direction` VALUES (67, 3, '2025-07-29 01:08:36');
INSERT INTO `student_direction` VALUES (67, 4, '2025-07-29 01:08:36');
INSERT INTO `student_direction` VALUES (68, 3, '2025-07-29 01:09:54');
INSERT INTO `student_direction` VALUES (68, 4, '2025-07-29 01:09:54');
INSERT INTO `student_direction` VALUES (69, 2, '2025-07-29 01:16:02');
INSERT INTO `student_direction` VALUES (69, 4, '2025-07-29 01:16:02');
INSERT INTO `student_direction` VALUES (69, 5, '2025-07-29 01:16:02');
INSERT INTO `student_direction` VALUES (70, 1, '2025-07-29 01:22:08');
INSERT INTO `student_direction` VALUES (70, 3, '2025-07-29 01:22:08');
INSERT INTO `student_direction` VALUES (70, 4, '2025-07-29 01:22:08');
INSERT INTO `student_direction` VALUES (71, 2, '2025-08-24 00:57:24');
INSERT INTO `student_direction` VALUES (71, 3, '2025-08-24 00:57:24');
INSERT INTO `student_direction` VALUES (71, 4, '2025-08-24 00:57:24');
INSERT INTO `student_direction` VALUES (72, 3, '2025-09-06 00:22:55');
INSERT INTO `student_direction` VALUES (72, 4, '2025-09-06 00:22:55');
INSERT INTO `student_direction` VALUES (72, 5, '2025-09-06 00:22:55');
INSERT INTO `student_direction` VALUES (73, 2, '2025-09-15 10:20:10');
INSERT INTO `student_direction` VALUES (73, 3, '2025-09-15 10:20:10');
INSERT INTO `student_direction` VALUES (73, 4, '2025-09-15 10:20:10');

-- ----------------------------
-- Table structure for studio_info
-- ----------------------------
DROP TABLE IF EXISTS `studio_info`;
CREATE TABLE `studio_info`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工作室名称',
  `establish_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '成立时间',
  `director` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人',
  `member_count` int NULL DEFAULT 0 COMMENT '成员数量',
  `project_count` int NULL DEFAULT 0 COMMENT '项目数量',
  `awards` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '获奖情况',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系邮箱',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `room` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '具体房间号',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_user` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工作室信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of studio_info
-- ----------------------------
INSERT INTO `studio_info` VALUES (1, '何湘技能大师工作室', '2025-08-15', '何湘', 28, 19, '12312312312312312', '0731-12345678', '1111111111', '重庆璧山区重庆机电职业技术大学', '综合楼A区305室', '2025-06-18 13:57:10', '2025-09-13 01:40:41', NULL, NULL);

-- ----------------------------
-- Table structure for sub_member
-- ----------------------------
DROP TABLE IF EXISTS `sub_member`;
CREATE TABLE `sub_member`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sub_task_id` bigint NOT NULL COMMENT '子任务ID，关联sub_task.sub_task_id',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID，关联user.user_id',
  `role` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '负责人/参与者',
  `note` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注信息',
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_subtask_user`(`sub_task_id` ASC, `user_id` ASC) USING BTREE COMMENT '确保用户在同一子任务中不重复',
  INDEX `fk_stm_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_stm_subtask` FOREIGN KEY (`sub_task_id`) REFERENCES `sub_task` (`sub_task_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_stm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 209 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '子任务成员关联表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sub_member
-- ----------------------------
INSERT INTO `sub_member` VALUES (161, 361, 1, '负责人', '加游', '2025-07-30 04:32:20');
INSERT INTO `sub_member` VALUES (162, 361, 2255, '参与者', 'sb', '2025-07-30 04:32:20');
INSERT INTO `sub_member` VALUES (163, 361, 2271, '参与者', '嘿嘿嘿', '2025-07-30 04:32:20');
INSERT INTO `sub_member` VALUES (164, 362, 2272, '参与者', '', '2025-07-30 04:38:21');
INSERT INTO `sub_member` VALUES (165, 362, 2255, '参与者', '', '2025-07-30 04:38:21');
INSERT INTO `sub_member` VALUES (166, 362, 1, '参与者', '', '2025-07-30 04:38:21');
INSERT INTO `sub_member` VALUES (167, 362, 2271, '参与者', '', '2025-07-30 04:38:21');
INSERT INTO `sub_member` VALUES (168, 363, 2272, '参与者', '', '2025-07-30 04:42:21');
INSERT INTO `sub_member` VALUES (169, 363, 2271, '参与者', '', '2025-07-30 04:42:21');
INSERT INTO `sub_member` VALUES (170, 363, 2255, '参与者', '', '2025-07-30 04:42:21');
INSERT INTO `sub_member` VALUES (171, 363, 1, '参与者', '', '2025-07-30 04:42:21');
INSERT INTO `sub_member` VALUES (172, 364, 2274, '负责人', '', '2025-07-30 11:26:25');
INSERT INTO `sub_member` VALUES (173, 364, 1, '参与者', '', '2025-07-30 11:26:25');
INSERT INTO `sub_member` VALUES (174, 364, 2271, '参与者', '', '2025-07-30 11:26:25');
INSERT INTO `sub_member` VALUES (175, 364, 2255, '参与者', '', '2025-07-30 11:26:25');
INSERT INTO `sub_member` VALUES (176, 365, 2274, '参与者', '', '2025-07-30 11:44:17');
INSERT INTO `sub_member` VALUES (177, 365, 2273, '参与者', '', '2025-07-30 11:44:17');
INSERT INTO `sub_member` VALUES (178, 365, 2271, '参与者', '', '2025-07-30 11:44:17');
INSERT INTO `sub_member` VALUES (179, 365, 2255, '参与者', '', '2025-07-30 11:44:17');
INSERT INTO `sub_member` VALUES (180, 365, 1, '参与者', '', '2025-07-30 11:44:17');
INSERT INTO `sub_member` VALUES (181, 366, 2271, '参与者', '', '2025-07-30 11:59:28');
INSERT INTO `sub_member` VALUES (182, 366, 2255, '参与者', '', '2025-07-30 11:59:28');
INSERT INTO `sub_member` VALUES (183, 366, 1, '参与者', '', '2025-07-30 11:59:28');
INSERT INTO `sub_member` VALUES (184, 367, 2272, '参与者', '', '2025-07-30 14:57:01');
INSERT INTO `sub_member` VALUES (185, 367, 2271, '参与者', '', '2025-07-30 14:57:01');
INSERT INTO `sub_member` VALUES (186, 367, 2255, '参与者', '', '2025-07-30 14:57:01');
INSERT INTO `sub_member` VALUES (187, 367, 1, '参与者', '', '2025-07-30 14:57:01');
INSERT INTO `sub_member` VALUES (188, 368, 1, '参与者', '123123', '2025-08-03 13:30:06');
INSERT INTO `sub_member` VALUES (189, 368, 2271, '参与者', '', '2025-08-03 13:30:06');
INSERT INTO `sub_member` VALUES (190, 368, 2272, '参与者', '', '2025-08-03 13:30:06');
INSERT INTO `sub_member` VALUES (194, 372, 1, '负责人', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (195, 372, 2271, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (196, 372, 2255, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (197, 372, 2274, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (198, 372, 2273, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (199, 373, 1, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (200, 373, 2271, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (201, 373, 2255, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (202, 373, 2274, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (203, 373, 2273, '参与者', '', '2025-08-24 01:07:59');
INSERT INTO `sub_member` VALUES (204, 374, 1, '参与者', '', '2025-09-02 16:30:42');
INSERT INTO `sub_member` VALUES (205, 374, 2255, '参与者', '', '2025-09-02 16:30:42');
INSERT INTO `sub_member` VALUES (206, 374, 2271, '参与者', '', '2025-09-02 16:30:42');
INSERT INTO `sub_member` VALUES (207, 374, 2283, '参与者', '', '2025-09-02 16:30:42');
INSERT INTO `sub_member` VALUES (208, 375, 2283, '参与者', '', '2025-09-05 23:40:13');

-- ----------------------------
-- Table structure for sub_task
-- ----------------------------
DROP TABLE IF EXISTS `sub_task`;
CREATE TABLE `sub_task`  (
  `sub_task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '子任务ID，主键',
  `task_id` bigint NOT NULL COMMENT '所属主任务ID，关联task.task_id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '子任务标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '子任务详细描述',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0=进行中，1=已完成，2=待审核，3=已退回\'',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `create_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_user` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`sub_task_id`) USING BTREE,
  INDEX `fk_sub_task_parent`(`task_id` ASC) USING BTREE,
  CONSTRAINT `fk_sub_task_parent` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 376 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '子任务信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sub_task
-- ----------------------------
INSERT INTO `sub_task` VALUES (361, 53, '买水', '花钱少点', 1, '2025-07-30 04:32:20', '2025-07-30 14:26:45', '1', '1');
INSERT INTO `sub_task` VALUES (362, 54, '123', '123', 1, '2025-07-30 04:38:21', '2025-07-30 14:26:42', '1', '1');
INSERT INTO `sub_task` VALUES (363, 55, '123', '123', 1, '2025-07-30 04:42:22', '2025-07-30 14:26:41', '1', '1');
INSERT INTO `sub_task` VALUES (364, 56, '123123', '12312312', 1, '2025-07-30 11:26:26', '2025-07-30 15:11:06', '1', '1');
INSERT INTO `sub_task` VALUES (365, 57, '213', '213', 0, '2025-07-30 11:44:18', '2025-07-30 11:44:18', '1', '1');
INSERT INTO `sub_task` VALUES (366, 58, '123', '123', 1, '2025-07-30 11:59:28', '2025-08-03 19:35:51', '1', '1');
INSERT INTO `sub_task` VALUES (367, 59, '123', '2133', 1, '2025-07-30 14:57:02', '2025-08-03 01:05:21', '1', '1');
INSERT INTO `sub_task` VALUES (368, 60, '1231', '123', 1, '2025-08-03 13:30:07', '2025-08-03 16:58:25', '1', '1');
INSERT INTO `sub_task` VALUES (369, 61, '123', '123', 0, '2025-08-03 19:19:44', '2025-08-03 19:19:44', '1', '1');
INSERT INTO `sub_task` VALUES (372, 63, '12321', '23123', 3, '2025-08-24 01:07:59', '2025-09-02 16:23:26', '1', '1');
INSERT INTO `sub_task` VALUES (373, 63, '12', '1231', 1, '2025-08-24 01:07:59', '2025-09-02 16:18:02', '1', '1');
INSERT INTO `sub_task` VALUES (374, 64, '子任务标题', '说明。。。。', 1, '2025-09-02 16:30:43', '2025-09-09 23:14:49', '1', '1');
INSERT INTO `sub_task` VALUES (375, 65, '123', '123', 2, '2025-09-05 23:40:14', '2025-09-15 15:47:59', '2282', '2282');

-- ----------------------------
-- Table structure for support_contact
-- ----------------------------
DROP TABLE IF EXISTS `support_contact`;
CREATE TABLE `support_contact`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '技术支持人员姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职位',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '状态(1-启用,0-禁用)',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '技术支持联系人表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of support_contact
-- ----------------------------
INSERT INTO `support_contact` VALUES (1, 'hhh', '17328487665', '3486687886@qq.com', '学生', '1', '2025-07-09 09:26:25', '2025-08-06 09:26:28');

-- ----------------------------
-- Table structure for system_notification
-- ----------------------------
DROP TABLE IF EXISTS `system_notification`;
CREATE TABLE `system_notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知类型：announcement(公告), task(任务), course(课程), system(系统通知)',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '通知内容',
  `source_id` bigint NULL DEFAULT NULL COMMENT '来源ID(如任务ID、公告ID、课程ID等)',
  `sender_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '发送者ID',
  `target_user_id` bigint UNSIGNED NULL DEFAULT NULL COMMENT '目标用户ID，NULL表示全局通知',
  `is_read` tinyint(1) NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `importance` tinyint NULL DEFAULT 0 COMMENT '重要程度：0普通，1重要，2紧急',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1有效，0已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_target_read`(`target_user_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_type_source`(`type` ASC, `source_id` ASC) USING BTREE,
  INDEX `idx_sender`(`sender_id` ASC) USING BTREE,
  CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_notification_target` FOREIGN KEY (`target_user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 162 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_notification
-- ----------------------------
INSERT INTO `system_notification` VALUES (1, 'announcement', '测试公告通知', '这是一条测试公告通知内容，用于测试通知功能。', 1, 1, NULL, 1, '2025-08-03 01:57:33', 1, 1, '2025-07-23 13:52:28');
INSERT INTO `system_notification` VALUES (2, 'announcement', '测试公告通知', '这是一条测试公告通知内容，用于测试通知功能。', 1, 1, NULL, 1, '2025-07-23 14:32:58', 1, 1, '2025-07-23 13:52:42');
INSERT INTO `system_notification` VALUES (3, 'announcement', '测试公告通知', '这是一条测试公告通知内容，用于测试通知功能。', 1, 1, NULL, 1, '2025-07-23 14:32:32', 1, 1, '2025-07-23 13:53:18');
INSERT INTO `system_notification` VALUES (4, 'announcement', '测试公告通知', '这是一条测试公告通知内容，用于测试通知功能。', 1, 1, NULL, 1, '2025-07-23 14:32:33', 1, 1, '2025-07-23 13:54:45');
INSERT INTO `system_notification` VALUES (5, 'system', '测试系统通知', '这是一条测试系统通知内容，用于测试通知功能。', NULL, NULL, NULL, 1, '2025-07-23 14:27:24', 1, 1, '2025-07-23 14:05:12');
INSERT INTO `system_notification` VALUES (6, 'system', '测试系统通知', '这是一条测试系统通知内容，用于测试通知功能。', NULL, NULL, NULL, 1, '2025-07-23 14:26:55', 1, 1, '2025-07-23 14:05:18');
INSERT INTO `system_notification` VALUES (7, 'system', '测试系统通知', '这是一条测试系统通知内容，用于测试通知功能。', NULL, NULL, NULL, 1, '2025-07-23 14:26:56', 1, 1, '2025-07-23 14:06:27');
INSERT INTO `system_notification` VALUES (8, 'system', '测试系统通知', '这是一条测试系统通知内容，用于测试通知功能。', NULL, NULL, NULL, 1, '2025-07-23 14:26:57', 1, 1, '2025-07-23 14:18:44');
INSERT INTO `system_notification` VALUES (9, 'system', '测试系统通知', '这是一条测试系统通知内容，用于测试通知功能。', NULL, NULL, NULL, 1, '2025-07-23 14:26:58', 1, 1, '2025-07-23 14:19:18');
INSERT INTO `system_notification` VALUES (10, 'task', '测试任务通知', '这是一条测试任务通知内容，用于测试通知功能。', 1, 1, NULL, 1, '2025-07-23 14:32:34', 1, 1, '2025-07-23 14:27:53');
INSERT INTO `system_notification` VALUES (11, 'system', '测试系统通知', '这是一条测试系统通知内容，用于测试通知功能。', NULL, NULL, NULL, 0, NULL, 1, 1, '2025-07-23 14:32:50');
INSERT INTO `system_notification` VALUES (12, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:57:40', 0, 1, '2025-07-25 01:00:00');
INSERT INTO `system_notification` VALUES (13, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:57:40', 0, 1, '2025-07-26 01:00:00');
INSERT INTO `system_notification` VALUES (14, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:57:40', 0, 1, '2025-07-27 01:00:00');
INSERT INTO `system_notification` VALUES (15, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:57:39', 0, 1, '2025-07-28 01:00:00');
INSERT INTO `system_notification` VALUES (16, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:57:37', 0, 1, '2025-07-29 01:00:00');
INSERT INTO `system_notification` VALUES (17, 'system', '考勤即将开始: null - 值班考勤', '考勤即将开始: null - 值班考勤\n开始时间: 2025-07-29T14:00\n结束时间: 2025-07-29T15:30\n地点: 工作室', NULL, NULL, NULL, 1, '2025-08-03 01:57:38', 1, 1, '2025-07-29 13:30:00');
INSERT INTO `system_notification` VALUES (18, 'system', '考勤即将开始: null - 值班考勤', '考勤即将开始: null - 值班考勤\n开始时间: 2025-07-29T14:00\n结束时间: 2025-07-29T15:30\n地点: 工作室', NULL, NULL, NULL, 1, '2025-08-03 01:57:36', 1, 1, '2025-07-29 13:40:00');
INSERT INTO `system_notification` VALUES (19, 'system', '考勤即将开始: null - 值班考勤', '考勤即将开始: null - 值班考勤\n开始时间: 2025-07-29T14:00\n结束时间: 2025-07-29T15:30\n地点: 工作室', NULL, NULL, NULL, 1, '2025-08-03 01:57:36', 1, 1, '2025-07-29 13:50:00');
INSERT INTO `system_notification` VALUES (20, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00.000Z\n截止时间: 2025-07-16T16:00:00.000Z', 47, 1, 1, 1, '2025-08-03 01:57:36', 1, 1, '2025-07-29 23:56:33');
INSERT INTO `system_notification` VALUES (21, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00.000Z\n截止时间: 2025-07-16T16:00:00.000Z', 47, 1, 2255, 0, NULL, 1, 1, '2025-07-29 23:56:33');
INSERT INTO `system_notification` VALUES (22, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00.000Z\n截止时间: 2025-07-16T16:00:00.000Z', 47, 1, 2272, 0, NULL, 1, 1, '2025-07-29 23:56:33');
INSERT INTO `system_notification` VALUES (23, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00.000Z\n截止时间: 2025-07-16T16:00:00.000Z', 47, 1, 2274, 0, NULL, 1, 1, '2025-07-29 23:56:33');
INSERT INTO `system_notification` VALUES (24, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00.000Z\n截止时间: 2025-07-16T16:00:00.000Z', 47, 1, 2273, 0, NULL, 1, 1, '2025-07-29 23:56:33');
INSERT INTO `system_notification` VALUES (25, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-29T15:57:07.000Z', 47, 1, 1, 0, NULL, 1, 1, '2025-07-29 23:57:09');
INSERT INTO `system_notification` VALUES (26, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-29T15:57:07.000Z', 47, 1, 2255, 0, NULL, 1, 1, '2025-07-29 23:57:09');
INSERT INTO `system_notification` VALUES (27, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-29T15:57:07.000Z', 47, 1, 2272, 0, NULL, 1, 1, '2025-07-29 23:57:09');
INSERT INTO `system_notification` VALUES (28, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-29T15:57:07.000Z', 47, 1, 2273, 0, NULL, 1, 1, '2025-07-29 23:57:09');
INSERT INTO `system_notification` VALUES (29, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-29T15:57:07.000Z', 47, 1, 2274, 0, NULL, 1, 1, '2025-07-29 23:57:09');
INSERT INTO `system_notification` VALUES (30, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-31T07:57:07.000Z', 47, 1, 1, 0, NULL, 1, 1, '2025-07-29 23:57:22');
INSERT INTO `system_notification` VALUES (31, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-31T07:57:07.000Z', 47, 1, 2255, 0, NULL, 1, 1, '2025-07-29 23:57:22');
INSERT INTO `system_notification` VALUES (32, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-31T07:57:07.000Z', 47, 1, 2272, 0, NULL, 1, 1, '2025-07-29 23:57:22');
INSERT INTO `system_notification` VALUES (33, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-31T07:57:07.000Z', 47, 1, 2273, 0, NULL, 1, 1, '2025-07-29 23:57:22');
INSERT INTO `system_notification` VALUES (34, 'task', '任务更新: 任务1', '您参与的任务已更新: 任务1\n子任务: 主任务1\n开始时间: 2025-07-07T16:00:00\n截止时间: 2025-07-31T07:57:07.000Z', 47, 1, 2274, 0, NULL, 1, 1, '2025-07-29 23:57:22');
INSERT INTO `system_notification` VALUES (35, 'task', '新任务分配: 1123123', '您被分配了一个新任务: 1123123\n子任务: 1231231\n开始时间: 2025-07-08T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 48, 1, 1, 0, NULL, 1, 1, '2025-07-30 00:20:07');
INSERT INTO `system_notification` VALUES (36, 'task', '新任务分配: 1123123', '您被分配了一个新任务: 1123123\n子任务: 1231231\n开始时间: 2025-07-08T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 48, 1, 2255, 0, NULL, 1, 1, '2025-07-30 00:20:07');
INSERT INTO `system_notification` VALUES (37, 'task', '新任务分配: 1123123', '您被分配了一个新任务: 1123123\n子任务: 1231231\n开始时间: 2025-07-08T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 48, 1, 2271, 0, NULL, 1, 1, '2025-07-30 00:20:07');
INSERT INTO `system_notification` VALUES (38, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 0, NULL, 0, 1, '2025-07-30 01:00:00');
INSERT INTO `system_notification` VALUES (39, 'task', '新任务分配: 123123123121', '您被分配了一个新任务: 123123123121\n子任务: 123123\n开始时间: 2025-07-15T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 49, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:05:34');
INSERT INTO `system_notification` VALUES (40, 'task', '新任务分配: 123123123121', '您被分配了一个新任务: 123123123121\n子任务: 123123\n开始时间: 2025-07-15T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 49, 1, 2272, 0, NULL, 1, 1, '2025-07-30 01:05:34');
INSERT INTO `system_notification` VALUES (41, 'task', '新任务分配: 123123123121', '您被分配了一个新任务: 123123123121\n子任务: 123123\n开始时间: 2025-07-15T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 49, 1, 2274, 0, NULL, 1, 1, '2025-07-30 01:05:34');
INSERT INTO `system_notification` VALUES (42, 'task', '任务状态变更: 1123123', '您参与的任务状态已变更: 1123123\n状态: 进行中', 48, 1, 1, 0, NULL, 1, 1, '2025-07-30 01:05:46');
INSERT INTO `system_notification` VALUES (43, 'task', '任务状态变更: 1123123', '您参与的任务状态已变更: 1123123\n状态: 进行中', 48, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:05:46');
INSERT INTO `system_notification` VALUES (44, 'task', '任务状态变更: 1123123', '您参与的任务状态已变更: 1123123\n状态: 进行中', 48, 1, 2271, 0, NULL, 1, 1, '2025-07-30 01:05:46');
INSERT INTO `system_notification` VALUES (45, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2272, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (46, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (47, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 1, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (48, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2271, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (49, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2273, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (50, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 1231231\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2273, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (51, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 1231231\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2272, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (52, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 1231231\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2271, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (53, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 1231231\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (54, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 1231231\n开始时间: 2025-06-23T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 50, 1, 1, 0, NULL, 1, 1, '2025-07-30 01:19:39');
INSERT INTO `system_notification` VALUES (55, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 2273, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (56, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 2272, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (57, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (58, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 1, 1, '2025-07-30 02:07:33', 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (59, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 2271, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (60, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 2272, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (61, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 2271, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (62, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (63, 'task', '新任务分配: 红红火火恍恍惚惚哈哈哈', '您被分配了一个新任务: 红红火火恍恍惚惚哈哈哈\n子任务: 123123\n开始时间: 2025-07-06T16:00:00.000Z\n截止时间: 2025-07-30T16:00:00.000Z', 51, 1, 1, 0, NULL, 1, 1, '2025-07-30 01:21:23');
INSERT INTO `system_notification` VALUES (64, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 1, 1, '2025-07-30 02:04:53', 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (65, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (66, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 2271, 0, NULL, 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (67, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 2272, 0, NULL, 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (68, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 2273, 0, NULL, 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (69, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 1, 1, '2025-08-03 01:53:19', 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (70, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 2255, 0, NULL, 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (71, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 2271, 0, NULL, 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (72, 'task', '任务状态变更: 红红火火恍恍惚惚哈哈哈', '您参与的任务状态已变更: 红红火火恍恍惚惚哈哈哈\n状态: 进行中', 51, 1, 2272, 0, NULL, 1, 1, '2025-07-30 01:21:34');
INSERT INTO `system_notification` VALUES (73, 'task', '任务审批通过', '您的任务已审批通过，审批意见：123123', 51, 1, NULL, 1, '2025-08-03 01:53:19', 1, 1, '2025-07-30 03:59:41');
INSERT INTO `system_notification` VALUES (74, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12312\n开始时间: 2025-07-30T20:09:31.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 52, 1, 2271, 0, NULL, 1, 1, '2025-07-30 04:09:49');
INSERT INTO `system_notification` VALUES (75, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12312\n开始时间: 2025-07-30T20:09:31.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 52, 1, 2273, 0, NULL, 1, 1, '2025-07-30 04:09:49');
INSERT INTO `system_notification` VALUES (76, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12312\n开始时间: 2025-07-30T20:09:31.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 52, 1, 2275, 0, NULL, 1, 1, '2025-07-30 04:09:49');
INSERT INTO `system_notification` VALUES (77, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12312\n开始时间: 2025-07-30T20:09:31.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 52, 1, 2255, 0, NULL, 1, 1, '2025-07-30 04:09:49');
INSERT INTO `system_notification` VALUES (78, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12312\n开始时间: 2025-07-30T20:09:31.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 52, 1, 1, 1, '2025-08-03 01:53:20', 1, 1, '2025-07-30 04:09:49');
INSERT INTO `system_notification` VALUES (79, 'task', '新任务分配: 开会', '您被分配了一个新任务: 开会\n子任务: 买水\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 53, 1, 1, 1, '2025-08-03 01:53:20', 1, 1, '2025-07-30 04:32:21');
INSERT INTO `system_notification` VALUES (80, 'task', '新任务分配: 开会', '您被分配了一个新任务: 开会\n子任务: 买水\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 53, 1, 2255, 0, NULL, 1, 1, '2025-07-30 04:32:21');
INSERT INTO `system_notification` VALUES (81, 'task', '新任务分配: 开会', '您被分配了一个新任务: 开会\n子任务: 买水\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 53, 1, 2271, 0, NULL, 1, 1, '2025-07-30 04:32:21');
INSERT INTO `system_notification` VALUES (82, 'task', '任务状态变更: 开会', '您参与的任务状态已变更: 开会\n状态: 已完成', 53, 1, 1, 1, '2025-08-03 01:53:21', 1, 1, '2025-07-30 04:37:29');
INSERT INTO `system_notification` VALUES (83, 'task', '任务状态变更: 开会', '您参与的任务状态已变更: 开会\n状态: 已完成', 53, 1, 2255, 0, NULL, 1, 1, '2025-07-30 04:37:29');
INSERT INTO `system_notification` VALUES (84, 'task', '任务状态变更: 开会', '您参与的任务状态已变更: 开会\n状态: 已完成', 53, 1, 2271, 0, NULL, 1, 1, '2025-07-30 04:37:29');
INSERT INTO `system_notification` VALUES (85, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 123\n开始时间: 2025-07-30T20:38:07.000Z\n截止时间: 2025-08-05T16:00:00.000Z', 54, 1, 2272, 0, NULL, 1, 1, '2025-07-30 04:38:21');
INSERT INTO `system_notification` VALUES (86, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 123\n开始时间: 2025-07-30T20:38:07.000Z\n截止时间: 2025-08-05T16:00:00.000Z', 54, 1, 2255, 0, NULL, 1, 1, '2025-07-30 04:38:21');
INSERT INTO `system_notification` VALUES (87, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 123\n开始时间: 2025-07-30T20:38:07.000Z\n截止时间: 2025-08-05T16:00:00.000Z', 54, 1, 1, 1, '2025-08-03 01:53:21', 1, 1, '2025-07-30 04:38:21');
INSERT INTO `system_notification` VALUES (88, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 123\n开始时间: 2025-07-30T20:38:07.000Z\n截止时间: 2025-08-05T16:00:00.000Z', 54, 1, 2271, 0, NULL, 1, 1, '2025-07-30 04:38:21');
INSERT INTO `system_notification` VALUES (89, 'task', '任务审批退回', '您的任务被退回，原因：gb\n', 54, 1, NULL, 1, '2025-08-03 01:53:22', 1, 1, '2025-07-30 04:39:11');
INSERT INTO `system_notification` VALUES (90, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123\n开始时间: 2025-08-06T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 55, 1, 2272, 0, NULL, 1, 1, '2025-07-30 04:42:22');
INSERT INTO `system_notification` VALUES (91, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123\n开始时间: 2025-08-06T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 55, 1, 2271, 0, NULL, 1, 1, '2025-07-30 04:42:22');
INSERT INTO `system_notification` VALUES (92, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123\n开始时间: 2025-08-06T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 55, 1, 2255, 0, NULL, 1, 1, '2025-07-30 04:42:22');
INSERT INTO `system_notification` VALUES (93, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123\n开始时间: 2025-08-06T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 55, 1, 1, 1, '2025-08-03 01:53:23', 1, 1, '2025-07-30 04:42:22');
INSERT INTO `system_notification` VALUES (94, 'task', '任务审批通过', '您的任务已审批通过，审批意见：123123', 55, 1, NULL, 1, '2025-08-03 01:53:23', 1, 1, '2025-07-30 04:42:44');
INSERT INTO `system_notification` VALUES (95, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 56, 1, 2274, 0, NULL, 1, 1, '2025-07-30 11:26:26');
INSERT INTO `system_notification` VALUES (96, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 56, 1, 1, 1, '2025-08-03 01:53:23', 1, 1, '2025-07-30 11:26:26');
INSERT INTO `system_notification` VALUES (97, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 56, 1, 2271, 0, NULL, 1, 1, '2025-07-30 11:26:26');
INSERT INTO `system_notification` VALUES (98, 'task', '新任务分配: 123123', '您被分配了一个新任务: 123123\n子任务: 123123\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 56, 1, 2255, 0, NULL, 1, 1, '2025-07-30 11:26:26');
INSERT INTO `system_notification` VALUES (99, 'task', '新任务分配: 123213', '您被分配了一个新任务: 123213\n子任务: 213\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 57, 1, 2274, 0, NULL, 1, 1, '2025-07-30 11:44:18');
INSERT INTO `system_notification` VALUES (100, 'task', '新任务分配: 123213', '您被分配了一个新任务: 123213\n子任务: 213\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 57, 1, 2273, 0, NULL, 1, 1, '2025-07-30 11:44:18');
INSERT INTO `system_notification` VALUES (101, 'task', '新任务分配: 123213', '您被分配了一个新任务: 123213\n子任务: 213\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 57, 1, 2271, 0, NULL, 1, 1, '2025-07-30 11:44:18');
INSERT INTO `system_notification` VALUES (102, 'task', '新任务分配: 123213', '您被分配了一个新任务: 123213\n子任务: 213\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 57, 1, 2255, 0, NULL, 1, 1, '2025-07-30 11:44:18');
INSERT INTO `system_notification` VALUES (103, 'task', '新任务分配: 123213', '您被分配了一个新任务: 123213\n子任务: 213\n开始时间: 2025-07-30T16:00:00.000Z\n截止时间: 2025-07-31T16:00:00.000Z', 57, 1, 1, 1, '2025-08-03 01:53:16', 1, 1, '2025-07-30 11:44:18');
INSERT INTO `system_notification` VALUES (104, 'task', '任务状态变更: 123213', '您参与的任务状态已变更: 123213\n状态: 已完成', 57, 1, 1, 1, '2025-07-31 12:32:19', 1, 1, '2025-07-30 11:50:20');
INSERT INTO `system_notification` VALUES (105, 'task', '任务状态变更: 123213', '您参与的任务状态已变更: 123213\n状态: 已完成', 57, 1, 2255, 0, NULL, 1, 1, '2025-07-30 11:50:20');
INSERT INTO `system_notification` VALUES (106, 'task', '任务状态变更: 123213', '您参与的任务状态已变更: 123213\n状态: 已完成', 57, 1, 2271, 0, NULL, 1, 1, '2025-07-30 11:50:20');
INSERT INTO `system_notification` VALUES (107, 'task', '任务状态变更: 123213', '您参与的任务状态已变更: 123213\n状态: 已完成', 57, 1, 2273, 0, NULL, 1, 1, '2025-07-30 11:50:20');
INSERT INTO `system_notification` VALUES (108, 'task', '任务状态变更: 123213', '您参与的任务状态已变更: 123213\n状态: 已完成', 57, 1, 2274, 0, NULL, 1, 1, '2025-07-30 11:50:20');
INSERT INTO `system_notification` VALUES (109, 'task', '新任务分配: 1231', '您被分配了一个新任务: 1231\n子任务: 123\n开始时间: 2025-07-31T16:00:00.000Z\n截止时间: 2025-08-07T16:00:00.000Z', 58, 1, 2271, 0, NULL, 1, 1, '2025-07-30 11:59:28');
INSERT INTO `system_notification` VALUES (110, 'task', '新任务分配: 1231', '您被分配了一个新任务: 1231\n子任务: 123\n开始时间: 2025-07-31T16:00:00.000Z\n截止时间: 2025-08-07T16:00:00.000Z', 58, 1, 2255, 0, NULL, 1, 1, '2025-07-30 11:59:28');
INSERT INTO `system_notification` VALUES (111, 'task', '新任务分配: 1231', '您被分配了一个新任务: 1231\n子任务: 123\n开始时间: 2025-07-31T16:00:00.000Z\n截止时间: 2025-08-07T16:00:00.000Z', 58, 1, 1, 1, '2025-07-31 12:32:19', 1, 1, '2025-07-30 11:59:28');
INSERT INTO `system_notification` VALUES (112, 'task', '新任务分配: 12321', '您被分配了一个新任务: 12321\n子任务: 123\n开始时间: 2025-07-31T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 59, 1, 2272, 0, NULL, 1, 1, '2025-07-30 14:57:02');
INSERT INTO `system_notification` VALUES (113, 'task', '新任务分配: 12321', '您被分配了一个新任务: 12321\n子任务: 123\n开始时间: 2025-07-31T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 59, 1, 2271, 0, NULL, 1, 1, '2025-07-30 14:57:02');
INSERT INTO `system_notification` VALUES (114, 'task', '新任务分配: 12321', '您被分配了一个新任务: 12321\n子任务: 123\n开始时间: 2025-07-31T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 59, 1, 2255, 0, NULL, 1, 1, '2025-07-30 14:57:02');
INSERT INTO `system_notification` VALUES (115, 'task', '新任务分配: 12321', '您被分配了一个新任务: 12321\n子任务: 123\n开始时间: 2025-07-31T16:00:00.000Z\n截止时间: 2025-08-08T16:00:00.000Z', 59, 1, 1, 1, '2025-07-31 12:32:20', 1, 1, '2025-07-30 14:57:02');
INSERT INTO `system_notification` VALUES (116, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-07-31 12:32:21', 0, 1, '2025-07-31 01:00:00');
INSERT INTO `system_notification` VALUES (117, 'announcement', '123', '21213', 79, 1, NULL, 1, '2025-07-31 12:32:20', 0, 1, '2025-07-31 01:19:32');
INSERT INTO `system_notification` VALUES (118, 'announcement', '123', '123123', 78, 1, NULL, 1, '2025-07-31 12:32:21', 0, 1, '2025-07-31 01:19:34');
INSERT INTO `system_notification` VALUES (119, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:53:13', 0, 1, '2025-08-01 15:28:46');
INSERT INTO `system_notification` VALUES (120, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:53:12', 0, 1, '2025-08-02 01:00:00');
INSERT INTO `system_notification` VALUES (121, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-03 01:53:12', 0, 1, '2025-08-03 01:00:00');
INSERT INTO `system_notification` VALUES (122, 'task', '新任务分配: 12312', '您被分配了一个新任务: 12312\n子任务: 1231\n开始时间: 2025-08-06T16:00:00.000Z\n截止时间: 2025-08-14T16:00:00.000Z', 60, 1, 1, 0, NULL, 1, 1, '2025-08-03 13:30:07');
INSERT INTO `system_notification` VALUES (123, 'task', '新任务分配: 12312', '您被分配了一个新任务: 12312\n子任务: 1231\n开始时间: 2025-08-06T16:00:00.000Z\n截止时间: 2025-08-14T16:00:00.000Z', 60, 1, 2271, 0, NULL, 1, 1, '2025-08-03 13:30:07');
INSERT INTO `system_notification` VALUES (124, 'task', '新任务分配: 12312', '您被分配了一个新任务: 12312\n子任务: 1231\n开始时间: 2025-08-06T16:00:00.000Z\n截止时间: 2025-08-14T16:00:00.000Z', 60, 1, 2272, 0, NULL, 1, 1, '2025-08-03 13:30:07');
INSERT INTO `system_notification` VALUES (126, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: qwe\n开始时间: 2025-08-04T13:47:12.000Z\n截止时间: 2025-08-20T13:47:10.000Z', 62, 1, 2271, 0, NULL, 1, 1, '2025-08-03 21:47:48');
INSERT INTO `system_notification` VALUES (127, 'task', '任务更新: 123', '您参与的任务已更新: 123\n子任务: qwe\n开始时间: 2025-08-04T13:47:12\n截止时间: 2025-08-20T13:47:10', 62, 1, 2271, 0, NULL, 1, 1, '2025-08-03 21:49:08');
INSERT INTO `system_notification` VALUES (128, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-04 23:55:35', 0, 1, '2025-08-04 01:00:00');
INSERT INTO `system_notification` VALUES (129, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-23 23:17:41', 0, 1, '2025-08-05 01:00:00');
INSERT INTO `system_notification` VALUES (130, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 1, '2025-08-23 23:17:40', 0, 1, '2025-08-08 01:00:00');
INSERT INTO `system_notification` VALUES (131, 'announcement', '123', '123123', 80, 1, NULL, 0, NULL, 0, 1, '2025-08-23 23:18:21');
INSERT INTO `system_notification` VALUES (132, 'course', '新课程发布: 12313', '新课程已发布: 12313\n授课教师: ycj\n上课地点: E425\n上课时间: 12312', 22, 1, NULL, 0, NULL, 1, 1, '2025-08-23 23:20:05');
INSERT INTO `system_notification` VALUES (133, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 0, NULL, 0, 1, '2025-08-24 01:00:00');
INSERT INTO `system_notification` VALUES (134, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12321\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 1, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (135, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12321\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2271, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (136, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12321\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2255, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (137, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12321\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2274, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (138, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12321\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2273, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (139, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 1, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (140, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2271, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (141, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2255, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (142, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2274, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (143, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 12\n开始时间: 2025-08-24T17:07:24.000Z\n截止时间: 2025-08-25T16:00:00.000Z', 63, 1, 2273, 0, NULL, 1, 1, '2025-08-24 01:07:59');
INSERT INTO `system_notification` VALUES (144, 'course', '新课程发布: 测试课程1', '新课程已发布: 测试课程1\n授课教师: ycj\n上课地点: 办公室\n上课时间: 每周一 早上', 23, 1, NULL, 0, NULL, 1, 1, '2025-09-02 16:25:58');
INSERT INTO `system_notification` VALUES (145, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 子任务标题\n开始时间: 2025-09-03T08:29:59.000Z\n截止时间: 2025-09-03T16:00:00.000Z', 64, 1, 1, 0, NULL, 1, 1, '2025-09-02 16:30:43');
INSERT INTO `system_notification` VALUES (146, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 子任务标题\n开始时间: 2025-09-03T08:29:59.000Z\n截止时间: 2025-09-03T16:00:00.000Z', 64, 1, 2255, 0, NULL, 1, 1, '2025-09-02 16:30:43');
INSERT INTO `system_notification` VALUES (147, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 子任务标题\n开始时间: 2025-09-03T08:29:59.000Z\n截止时间: 2025-09-03T16:00:00.000Z', 64, 1, 2271, 0, NULL, 1, 1, '2025-09-02 16:30:43');
INSERT INTO `system_notification` VALUES (148, 'task', '新任务分配: 任务1', '您被分配了一个新任务: 任务1\n子任务: 子任务标题\n开始时间: 2025-09-03T08:29:59.000Z\n截止时间: 2025-09-03T16:00:00.000Z', 64, 1, 2283, 0, NULL, 1, 1, '2025-09-02 16:30:43');
INSERT INTO `system_notification` VALUES (149, 'announcement', '华为培训', '此处为内容说明。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。', 81, 1, NULL, 0, NULL, 0, 1, '2025-09-02 16:31:43');
INSERT INTO `system_notification` VALUES (150, 'task', '新任务分配: 123', '您被分配了一个新任务: 123\n子任务: 123\n开始时间: 2025-09-06T15:39:49.000Z\n截止时间: 2025-09-07T16:00:00.000Z', 65, 2282, 2283, 0, NULL, 1, 1, '2025-09-05 23:40:14');
INSERT INTO `system_notification` VALUES (151, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 0, NULL, 0, 1, '2025-09-06 01:00:00');
INSERT INTO `system_notification` VALUES (152, 'announcement', 'qwe', 'qwe', 75, 2282, NULL, 0, NULL, 0, 1, '2025-09-06 01:37:34');
INSERT INTO `system_notification` VALUES (153, 'announcement', '123123', '121233', 77, 2282, NULL, 0, NULL, 0, 1, '2025-09-06 01:37:35');
INSERT INTO `system_notification` VALUES (154, 'announcement', '123123', '12313213', 82, 2282, NULL, 1, '2025-09-10 00:50:21', 0, 1, '2025-09-06 01:38:52');
INSERT INTO `system_notification` VALUES (155, 'announcement', '12313', '1231', 83, 2282, NULL, 1, '2025-09-10 00:50:21', 0, 1, '2025-09-06 01:39:20');
INSERT INTO `system_notification` VALUES (156, 'announcement', '华为培训', '此处为内容说明。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。', 81, 2282, NULL, 1, '2025-09-10 00:50:20', 0, 1, '2025-09-06 01:39:31');
INSERT INTO `system_notification` VALUES (157, 'announcement', '123', '123123', 78, 1, NULL, 1, '2025-09-10 00:50:19', 0, 1, '2025-09-10 00:49:32');
INSERT INTO `system_notification` VALUES (158, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 0, NULL, 0, 1, '2025-09-11 01:00:00');
INSERT INTO `system_notification` VALUES (159, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 0, NULL, 0, 1, '2025-09-12 01:00:00');
INSERT INTO `system_notification` VALUES (160, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 0, NULL, 0, 1, '2025-09-16 01:00:01');
INSERT INTO `system_notification` VALUES (161, 'system', '系统任务状态更新', '系统已更新所有逾期任务的状态。\n请相关人员及时处理已逾期的任务。', NULL, NULL, NULL, 0, NULL, 0, 1, '2025-09-17 01:00:00');

-- ----------------------------
-- Table structure for task
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task`  (
  `task_id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID，主键',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '任务详细描述',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '任务状态：REJECTED=被退回，PENDING_REVIEW=待审核，OVERDUE=已逾期，COMPLETED=已完成，URGENT=紧急，IN_PROGRESS=进行中',
  `start_time` datetime NOT NULL COMMENT '任务开始日期',
  `end_time` datetime NOT NULL COMMENT '任务截止日期',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `create_user` bigint NULL DEFAULT NULL COMMENT '创建人',
  `update_user` bigint NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`task_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 66 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务信息主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task
-- ----------------------------
INSERT INTO `task` VALUES (53, '开会', '于2023年开会', 'COMPLETED', '2025-07-30 16:00:00', '2025-07-31 16:00:00', NULL, '2025-08-03 18:42:21', 1, 1);
INSERT INTO `task` VALUES (54, '123', '1231', 'COMPLETED', '2025-07-30 20:38:07', '2025-08-05 16:00:00', NULL, '2025-08-03 18:42:21', 1, 1);
INSERT INTO `task` VALUES (55, '123123', '123123', 'COMPLETED', '2025-08-06 16:00:00', '2025-08-08 16:00:00', NULL, '2025-08-03 18:42:22', 1, 1);
INSERT INTO `task` VALUES (56, '123123', '123312312', 'COMPLETED', '2025-07-30 16:00:00', '2025-07-31 16:00:00', NULL, '2025-08-03 18:42:23', 1, NULL);
INSERT INTO `task` VALUES (57, '123213', '123213', 'COMPLETED', '2025-07-30 16:00:00', '2025-07-31 16:00:00', '2025-07-30 11:44:18', '2025-07-30 11:50:20', 1, 1);
INSERT INTO `task` VALUES (58, '1231', '123123', 'COMPLETED', '2025-07-31 16:00:00', '2025-08-07 16:00:00', '2025-07-30 11:59:28', '2025-08-03 19:35:52', 1, 1);
INSERT INTO `task` VALUES (59, '12321', '123123', 'COMPLETED', '2025-07-31 16:00:00', '2025-08-08 16:00:00', '2025-07-30 14:57:02', '2025-08-03 01:05:21', 1, 1);
INSERT INTO `task` VALUES (60, '12312', '3123123', 'COMPLETED', '2025-08-06 16:00:00', '2025-08-14 16:00:00', '2025-08-03 13:30:07', '2025-08-03 16:58:26', 1, 1);
INSERT INTO `task` VALUES (61, '123', '123', 'OVERDUE', '2025-08-05 11:19:16', '2025-08-22 11:19:18', '2025-08-03 19:19:44', '2025-08-24 01:00:00', 1, 1);
INSERT INTO `task` VALUES (63, '123', '123123', 'OVERDUE', '2025-08-24 17:07:24', '2025-08-25 16:00:00', '2025-08-24 01:07:59', '2025-09-06 01:00:00', 1, 1);
INSERT INTO `task` VALUES (64, '任务1', '任务描述', 'COMPLETED', '2025-09-03 08:29:59', '2025-09-03 16:00:00', '2025-09-02 16:30:43', '2025-09-09 23:14:49', 1, 1);
INSERT INTO `task` VALUES (65, '123', '1231231', 'OVERDUE', '2025-09-06 15:39:49', '2025-09-07 16:00:00', '2025-09-05 23:40:14', '2025-09-16 01:00:00', 2282, 2282);

-- ----------------------------
-- Table structure for task_attachment
-- ----------------------------
DROP TABLE IF EXISTS `task_attachment`;
CREATE TABLE `task_attachment`  (
  `attachment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `task_id` bigint NOT NULL COMMENT '所属主任务ID',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件原始名称',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文件存储相对路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件MIME类型',
  `uploader_id` bigint UNSIGNED NOT NULL COMMENT '上传者ID',
  `upload_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`attachment_id`) USING BTREE,
  INDEX `idx_task_id`(`task_id` ASC) USING BTREE,
  CONSTRAINT `fk_attachment_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`task_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务附件表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task_attachment
-- ----------------------------
INSERT INTO `task_attachment` VALUES (13, 53, '图片1.png', 'task/attachment/2025/07/30/4fb1cd20c6b74642a816f6176a090e6b.png', 552519, 'image/png', 1, '2025-07-30 04:32:21');
INSERT INTO `task_attachment` VALUES (14, 53, '新建 Microsoft PowerPoint 演示文稿.pptx', 'task/attachment/2025/07/30/c28dedc157e542db8a35cd8dbaa118ac.pptx', 130633, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 1, '2025-07-30 04:32:21');
INSERT INTO `task_attachment` VALUES (15, 56, '3.实训项目讲解.pptx', 'task/attachment/2025/07/30/205e02e62a554c8482a94dc8f670403b.pptx', 263897, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 1, '2025-07-30 11:26:26');
INSERT INTO `task_attachment` VALUES (16, 57, 'Spring Cloud.docx', 'task/attachment/2025/07/30/16c947fefc59458481e6420a4aac2696.docx', 3546002, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1, '2025-07-30 11:44:18');
INSERT INTO `task_attachment` VALUES (17, 58, '项目简易步骤.txt', 'task/attachment/2025/07/30/fc407fc000ad4efdbbc7c3cab235237f.txt', 528, 'text/plain', 1, '2025-07-30 11:59:28');
INSERT INTO `task_attachment` VALUES (18, 59, '3.实训项目讲解.pptx', 'task/attachment/2025/07/30/0ed0610f3e8a43ec9a364c31e4097af0.pptx', 263897, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', 1, '2025-07-30 14:57:02');
INSERT INTO `task_attachment` VALUES (19, 60, 'sql.xlsx', 'task/attachment/2025/08/03/10b444ca9a1248fba57f37aac0fbd2b4.xlsx', 10900, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 1, '2025-08-03 13:30:07');
INSERT INTO `task_attachment` VALUES (20, 61, '课表.png', 'task/attachment/2025/08/03/eb678a46ee134bda9eca077b86168367.png', 1092189, 'image/png', 1, '2025-08-03 19:19:44');
INSERT INTO `task_attachment` VALUES (23, 63, '头像.png', 'task/attachment/2025/08/24/72f4a5bef0b44ba8a17c3af93c577699.png', 201740, 'image/png', 1, '2025-08-24 01:07:59');
INSERT INTO `task_attachment` VALUES (24, 63, '前端实现方案.docx', 'task/attachment/2025/08/24/43a3731238f54cc584b5175133b90192.docx', 1588168, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1, '2025-08-24 01:07:59');
INSERT INTO `task_attachment` VALUES (25, 64, '课表.png', 'task/attachment/2025/09/02/4369142ebc4845fe9f9f3930b27e0a3b.png', 1092189, 'image/png', 1, '2025-09-02 16:30:43');
INSERT INTO `task_attachment` VALUES (26, 64, '第15期总天数统计表(1) 的副本.pdf', 'task/attachment/2025/09/02/0124fd3fab504e44bf656273a411bcfe.pdf', 960900, 'application/pdf', 1, '2025-09-02 16:30:43');

-- ----------------------------
-- Table structure for task_submission
-- ----------------------------
DROP TABLE IF EXISTS `task_submission`;
CREATE TABLE `task_submission`  (
  `submission_id` bigint NOT NULL AUTO_INCREMENT COMMENT '提交ID，主键',
  `sub_task_id` bigint NOT NULL COMMENT '子任务ID，关联sub_task.sub_task_id',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '提交者ID，关联user.user_id',
  `submission_notice` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提交说明',
  `status` int NULL DEFAULT NULL COMMENT '提交状态(0=进行中,1=已完成,2=待审核,3=已退回)',
  `submission_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `review_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核备注或退回原因',
  `review_time` datetime NULL DEFAULT NULL COMMENT '审核操作时间',
  PRIMARY KEY (`submission_id`) USING BTREE,
  UNIQUE INDEX `uk_subtask_user`(`sub_task_id` ASC, `user_id` ASC) USING BTREE COMMENT '确保用户对同一子任务只能提交一次',
  INDEX `fk_submission_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_sub_task_id`(`sub_task_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_submission_sub_task` FOREIGN KEY (`sub_task_id`) REFERENCES `sub_task` (`sub_task_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_submission_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 2079 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '任务提交记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task_submission
-- ----------------------------
INSERT INTO `task_submission` VALUES (2068, 361, 2271, '我买好啦', 1, '2025-07-30 04:35:53', '审核通过', '2025-07-30 14:26:45');
INSERT INTO `task_submission` VALUES (2069, 362, 2271, '嘿嘿', 1, '2025-07-30 04:38:56', '审核通过', '2025-07-30 14:26:42');
INSERT INTO `task_submission` VALUES (2070, 363, 2271, '哈哈哈', 1, '2025-07-30 04:42:36', '审核通过', '2025-07-30 14:26:41');
INSERT INTO `task_submission` VALUES (2071, 364, 2271, 'cnm', 1, '2025-07-30 15:10:46', '可以', '2025-07-30 15:11:06');
INSERT INTO `task_submission` VALUES (2072, 366, 2271, '草泥马', 1, '2025-08-03 18:37:53', '审核通过', '2025-08-03 19:35:52');
INSERT INTO `task_submission` VALUES (2073, 367, 2271, '好好', 1, '2025-07-30 21:28:26', '审核通过', '2025-08-03 01:05:21');
INSERT INTO `task_submission` VALUES (2074, 368, 2271, '试试', 1, '2025-08-03 14:06:34', '审核通过', '2025-08-03 16:58:26');
INSERT INTO `task_submission` VALUES (2075, 372, 2271, '哈哈哈', 3, '2025-09-02 16:16:40', '你呢', '2025-09-02 16:23:27');
INSERT INTO `task_submission` VALUES (2076, 373, 2271, '哈哈哈', 1, '2025-09-02 16:16:53', 'jj', '2025-09-02 16:18:02');
INSERT INTO `task_submission` VALUES (2077, 374, 2283, '现在完成任务了', 1, '2025-09-09 23:14:03', '审核通过', '2025-09-09 23:14:49');
INSERT INTO `task_submission` VALUES (2078, 375, 2283, '12312', 2, '2025-09-15 15:48:00', NULL, NULL);

-- ----------------------------
-- Table structure for task_submission_attachment
-- ----------------------------
DROP TABLE IF EXISTS `task_submission_attachment`;
CREATE TABLE `task_submission_attachment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL COMMENT '提交ID',
  `file_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `file_path` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `file_type` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_submission_id`(`submission_id` ASC) USING BTREE,
  CONSTRAINT `task_submission_attachment_ibfk_1` FOREIGN KEY (`submission_id`) REFERENCES `task_submission` (`submission_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 114 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '工作提交附件关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task_submission_attachment
-- ----------------------------
INSERT INTO `task_submission_attachment` VALUES (102, 2068, 'sql.xlsx', 'task/submission/2025/07/30/5890a387638c472da6c835197af3dd7c.xlsx', 10900, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '2025-07-30 04:35:53');
INSERT INTO `task_submission_attachment` VALUES (103, 2069, 'sql.xlsx', 'task/submission/2025/07/30/fbe345d0b5be480faf0aba2635f39e92.xlsx', 10900, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '2025-07-30 04:38:56');
INSERT INTO `task_submission_attachment` VALUES (104, 2070, 'sql.xlsx', 'task/submission/2025/07/30/c502c5ff05c04215b115677da7f83f21.xlsx', 10900, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '2025-07-30 04:42:36');
INSERT INTO `task_submission_attachment` VALUES (105, 2071, '3.实训项目讲解.pptx', 'task/submission/2025/07/30/7255b5172d8f40d1b32c1be170568e68.pptx', 263897, 'application/vnd.openxmlformats-officedocument.presentationml.presentation', '2025-07-30 11:27:33');
INSERT INTO `task_submission_attachment` VALUES (106, 2072, '头像.png', 'task/submission/2025/07/30/191958b4d3824ae5a2dea51620eb0b52.png', 201740, 'image/png', '2025-07-30 21:28:05');
INSERT INTO `task_submission_attachment` VALUES (107, 2073, '1672.jpg', 'task/submission/2025/07/30/de96d32cab43479a815eafd6624c40ec.jpg', 548080, 'image/jpeg', '2025-07-30 21:28:26');
INSERT INTO `task_submission_attachment` VALUES (108, 2074, '9605.jpg', 'task/submission/2025/08/03/8d9ea2b19e984f91a33161ef0418e78d.jpg', 103254, 'image/jpeg', '2025-08-03 14:06:34');
INSERT INTO `task_submission_attachment` VALUES (109, 2075, '7.jpg', 'task/submission/2025/09/02/baf7c056716748b498db402b888c10aa.jpg', 79669, 'image/jpeg', '2025-09-02 16:13:39');
INSERT INTO `task_submission_attachment` VALUES (110, 2076, '9.jpg', 'task/submission/2025/09/02/6010eab38c5e463698e83d3371708593.jpg', 76289, 'image/jpeg', '2025-09-02 16:13:50');
INSERT INTO `task_submission_attachment` VALUES (112, 2077, '14.jpg', 'task/submission/2025/09/09/7b12df774df54e4092f6ccc7cdc939a5.jpg', 156975, 'image/jpeg', '2025-09-09 23:14:03');
INSERT INTO `task_submission_attachment` VALUES (113, 2078, '屏幕截图 2025-08-22 142227.png', 'task/submission/2025/09/15/588a6184ea8c440f82cefbc0119b7326.png', 63624, 'image/png', '2025-09-15 15:48:00');

-- ----------------------------
-- Table structure for teacher
-- ----------------------------
DROP TABLE IF EXISTS `teacher`;
CREATE TABLE `teacher`  (
  `teacher_id` bigint NOT NULL AUTO_INCREMENT COMMENT '老师id',
  `user_id` bigint UNSIGNED NOT NULL,
  `direction_id` bigint NOT NULL COMMENT '培训id',
  `office_location` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '办公室位置',
  `title` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '职称',
  PRIMARY KEY (`teacher_id`) USING BTREE,
  INDEX `teu`(`user_id` ASC) USING BTREE,
  CONSTRAINT `teu` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 11125 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher
-- ----------------------------
INSERT INTO `teacher` VALUES (11123, 2255, 1, 'E425', '123123');

-- ----------------------------
-- Table structure for teacher_direction
-- ----------------------------
DROP TABLE IF EXISTS `teacher_direction`;
CREATE TABLE `teacher_direction`  (
  `teacher_id` bigint NOT NULL COMMENT '老师ID（与teacher表一致）',
  `direction_id` bigint NOT NULL COMMENT '方向ID（与training_direction表一致）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
  PRIMARY KEY (`teacher_id`, `direction_id`) USING BTREE,
  INDEX `direction_id`(`direction_id` ASC) USING BTREE,
  CONSTRAINT `teacher_direction_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`teacher_id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `teacher_direction_ibfk_2` FOREIGN KEY (`direction_id`) REFERENCES `training_direction` (`direction_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher_direction
-- ----------------------------
INSERT INTO `teacher_direction` VALUES (11123, 1, '2025-07-16 17:13:08');

-- ----------------------------
-- Table structure for training_direction
-- ----------------------------
DROP TABLE IF EXISTS `training_direction`;
CREATE TABLE `training_direction`  (
  `direction_id` bigint NOT NULL AUTO_INCREMENT COMMENT '方向ID（无UNSIGNED）',
  `direction_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '方向名称',
  `description` varchar(200) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '方向描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`direction_id`) USING BTREE,
  UNIQUE INDEX `direction_name`(`direction_name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of training_direction
-- ----------------------------
INSERT INTO `training_direction` VALUES (1, '前端开发', 'Web前端技术与交互设计', '2025-06-07 03:19:16');
INSERT INTO `training_direction` VALUES (2, '后端开发', '服务器端编程与API开发', '2025-06-07 03:19:16');
INSERT INTO `training_direction` VALUES (3, '移动开发', 'iOS/Android应用开发', '2025-06-07 03:19:16');
INSERT INTO `training_direction` VALUES (4, '数据科学', '数据分析与机器学习', '2025-06-07 03:19:16');
INSERT INTO `training_direction` VALUES (5, 'UI/UX设计', '用户界面与用户体验设计', '2025-06-07 03:19:16');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `user_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `role_id` bigint NULL DEFAULT NULL COMMENT '0访客/1学员/2老师/3管理员',
  `user_name` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
  `name` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '0女1男',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `createUser` bigint NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  `updateUser` bigint NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '头像存储路径/URL',
  `status` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `position_id` smallint UNSIGNED NULL DEFAULT NULL COMMENT '职位ID',
  `email` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '邮箱',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `weiy`(`user_name` ASC) USING BTREE,
  INDEX `position_id`(`position_id` ASC) USING BTREE,
  INDEX `role_id`(`role_id` ASC) USING BTREE,
  CONSTRAINT `user_ibfk_1` FOREIGN KEY (`position_id`) REFERENCES `position` (`position_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_role_fk` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2285 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 3, 'admin', '202cb962ac59075b964b07152d234b70', 'hhh', '1', '17328487665', '2025-06-10 03:08:34', 123, '2025-07-13 16:09:06', 2187, 'avatar/student/2025/07/13/efae9f62d2b14a11a74db52de039b2eb.png', '1', 7, '123@qq.com');
INSERT INTO `user` VALUES (2255, 2, '123123', '4297f44b13955235245b2497399d7a93', 'ycj', '1', '17328487665', '2025-07-13 16:08:17', 2187, '2025-07-16 17:13:09', 1, 'avatar/teacher/2025/07/13/79874662300b4bec932dc1b7b0099bf8.png', '1', 5, '3486687886@qq.com');
INSERT INTO `user` VALUES (2271, 1, 'ycj502', '202cb962ac59075b964b07152d234b70', '123', '1', '17328487665', '2025-07-18 02:16:48', NULL, '2025-07-18 02:16:48', NULL, 'avatar/student/2025/07/18/72abbd2534b14eaaaf858cb5bf5b5fcd.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2272, 1, 'ycj00', '25f9e794323b453885f5181f1b624d0b', '学生1', '1', '17328487665', '2025-07-23 08:55:23', 1, '2025-07-23 08:58:37', 1, 'avatar/student/2025/07/23/e39aeeb8cb8148bdb01feac262f5f027.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2273, 1, 'hhh', '91c077fb33f3804d5c1e34e4a6e1e7ac', '学生2', '1', '17328487665', '2025-07-23 09:12:21', 2272, '2025-07-23 09:12:21', 2272, 'avatar/student/2025/07/23/320591da6053496b8719c420417588a4.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2274, 1, 'qq123', '25f9e794323b453885f5181f1b624d0b', '王刚', '1', '17328487665', '2025-07-29 00:56:42', 1, '2025-07-29 00:56:42', 1, 'avatar/student/2025/07/29/680669e992b9441eb3ebd62fc710a46d.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2275, 1, 'qqq123', '25f9e794323b453885f5181f1b624d0b', '老李', '1', '17328487665', '2025-07-29 01:02:06', 1, '2025-07-29 01:08:36', 1, 'avatar/student/2025/07/29/11532dd237724fb3aa2564a205de22bb.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2276, 1, 'qwe12313123', 'e10adc3949ba59abbe56e057f20f883e', '123123', '1', '17328487665', '2025-07-29 01:09:54', 1, '2025-07-29 01:09:54', 1, 'avatar/student/2025/07/29/02a51f01e652417a9d7da55ef70e95b0.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2277, 1, '1231eww', 'e10adc3949ba59abbe56e057f20f883e', '嘿嘿嘿', '1', '17328487665', '2025-07-29 01:16:02', 1, '2025-07-29 01:16:02', 1, 'avatar/student/2025/07/29/eececdd1b0a3445ebaa4c5e631db85e8.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2278, 1, 'asdqw', 'e10adc3949ba59abbe56e057f20f883e', '狗蛋', '1', '17328487665', '2025-07-29 01:22:08', 1, '2025-07-29 01:22:08', 1, 'avatar/student/2025/07/29/2b89b6e0d23a49319ff58162762db17d.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2279, 0, 'saqe', 'e10adc3949ba59abbe56e057f20f883e', 'asdadd', '1', '17328487665', '2025-07-30 19:03:18', 1, '2025-07-30 19:03:18', 1, 'temp/2025/07/30/3ac64e311e0a4232adeb7ede12de143f.png', '1', 0, '123@qq.com');
INSERT INTO `user` VALUES (2281, 1, '123', '101193d7181cc88340ae5b2b17bba8a1', '123', '1', '17328487665', '2025-08-24 00:57:24', 1, '2025-08-24 00:57:24', 1, 'avatar/student/2025/08/24/b4ccd70211ae4a31a30e248eea5f0e23.jpg', '1', 1, '123@qq.com');
INSERT INTO `user` VALUES (2282, 4, 'admin1', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '1', '17328487665', '2025-09-02 16:27:35', 1, '2025-09-02 16:27:35', 1, 'temp/2025/09/02/6e4e3530d6fe44dab46318bd9bf1bfc2.jfif', '1', 8, '3486687886@qq.com');
INSERT INTO `user` VALUES (2283, 1, 'xue', 'e10adc3949ba59abbe56e057f20f883e', '王刚', '1', '17328487665', '2025-09-02 16:28:57', 1, '2025-09-06 00:22:55', 2282, 'avatar/student/2025/09/02/e139fd3625594e0d83172ae0b65f3bb0.png', '1', 1, '3486687886@qq.com');
INSERT INTO `user` VALUES (2284, 1, '123456', 'f1887d3f9e6ee7a32fe5e76f4ab80d63', '123', '1', '17328487665', '2025-09-15 10:20:10', 1, '2025-09-15 10:20:10', 1, 'avatar/student/2025/09/15/f0c2174ce9d647ef8b1eaf8481729aa0.png', '1', 1, '3486687886@qq.com');

-- ----------------------------
-- Table structure for user_certificate
-- ----------------------------
DROP TABLE IF EXISTS `user_certificate`;
CREATE TABLE `user_certificate`  (
  `certificate_id` bigint NOT NULL AUTO_INCREMENT COMMENT '证书ID',
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `certificate_name` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '证书名称',
  `certificate_level` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '证书类型',
  `issue_org` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '颁发机构',
  `issue_date` date NULL DEFAULT NULL COMMENT '颁发日期',
  `expiry_date` date NULL DEFAULT NULL COMMENT '有效期至',
  `certificate_no` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '证书编号',
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '证书描述',
  `image_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '证书图片路径',
  `verification_url` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '证书验证链接',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_user` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_user` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '更新者',
  PRIMARY KEY (`certificate_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `user_certificate_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 57 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户证书表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_certificate
-- ----------------------------
INSERT INTO `user_certificate` VALUES (28, 1, '计算机二级', '专业能力相关证书', '教育部', '2025-07-23', NULL, '12321312', '没用', 'user/certificate/2025/07/13/b817311fe70f4140acd6760a21030416.png', NULL, '2025-07-13 16:01:36', NULL, '2025-07-13 16:01:36', NULL);
INSERT INTO `user_certificate` VALUES (29, 1, 'Python程序设计师', '高级', '中国电子学会', '2024-11-15', '2027-11-15', 'PY20241115001', 'Python编程能力认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (30, 1, '软件测试工程师', '中级', '工信部', '2024-08-20', '2027-08-20', 'ST20240820002', '软件测试专业能力证书', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (31, 2255, 'Java高级开发工程师', '高级', 'Oracle官方', '2023-09-10', '2026-09-10', 'JA20230910003', '企业级Java开发认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (32, 2255, '系统架构设计师', '高级', '软考办', '2023-05-15', NULL, 'SA20230515004', '高级系统架构设计能力', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (33, 2255, 'AWS解决方案架构师', '专业级', 'Amazon', '2024-03-12', '2027-03-12', 'AWS20240312005', '云架构设计专业认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (34, 2271, '前端开发工程师', '中级', '腾讯学院', '2024-06-25', '2027-06-25', 'FE20240625006', 'Vue.js/React开发能力认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (35, 2272, '数据分析师', '初级', '阿里云', '2024-12-01', '2027-12-01', 'DA20241201007', '数据处理与分析能力', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (36, 2272, 'MySQL数据库管理员', '中级', 'Oracle', '2024-10-18', '2027-10-18', 'MY20241018008', '数据库设计与管理', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (37, 2273, 'UI/UX设计师', '中级', '站酷高高手', '2024-09-30', '2027-09-30', 'UI20240930009', '用户界面设计专业认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (38, 2273, 'Adobe认证设计师', '专业级', 'Adobe', '2024-07-22', '2027-07-22', 'AD20240722010', 'Photoshop/Illustrator专业认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (39, 2274, '移动应用开发工程师', '中级', '华为开发者联盟', '2024-11-05', '2027-11-05', 'MA20241105011', 'Android/iOS开发认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (40, 2274, '鸿蒙应用开发者', '初级', '华为', '2025-01-15', '2028-01-15', 'HM20250115012', 'HarmonyOS应用开发', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (41, 2275, '网络安全工程师', '中级', '360安全学院', '2024-08-12', '2027-08-12', 'NS20240812013', '网络安全防护能力认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (42, 2275, 'Linux系统管理员', '高级', 'RedHat', '2024-04-18', '2027-04-18', 'LX20240418014', 'Linux服务器运维管理', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (43, 2276, '机器学习工程师', '初级', '百度AI学院', '2024-10-25', '2027-10-25', 'ML20241025015', '机器学习算法应用', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (44, 2277, '区块链开发工程师', '初级', '蚂蚁链学院', '2024-12-08', '2027-12-08', 'BC20241208016', '区块链技术应用开发', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (45, 2277, '智能合约开发', '中级', '以太坊基金会', '2025-02-14', '2028-02-14', 'SC20250214017', '智能合约编程能力', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (46, 2278, '大数据开发工程师', '中级', '阿里云', '2024-09-16', '2027-09-16', 'BD20240916018', 'Spark/Hadoop大数据处理', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (47, 2278, 'Kafka消息中间件', '专业级', 'Apache', '2024-11-28', '2027-11-28', 'KF20241128019', '分布式消息处理专家', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (48, 2279, '运维工程师', '中级', '腾讯云', '2024-07-30', '2027-07-30', 'OP20240730020', '云平台运维管理认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (49, 2281, '游戏开发工程师', '初级', 'Unity官方', '2024-12-15', '2027-12-15', 'GD20241215021', 'Unity3D游戏开发', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (50, 2282, '项目管理专业人士PMP', '专业级', 'PMI', '2023-03-20', '2026-03-20', 'PM20230320022', '项目管理专业认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (51, 2282, 'ITIL服务管理专家', '专家级', 'AXELOS', '2023-06-15', NULL, 'IT20230615023', 'IT服务管理最佳实践', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (52, 2282, 'CISSP信息安全专家', '专家级', '(ISC)²', '2022-11-10', '2025-11-10', 'CI20221110024', '信息安全专业人士', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (53, 2283, 'Java开发工程师', '高级', 'Oracle公司', '2025-05-15', '2028-05-15', 'JAVA202505150123', '具备Java企业级开发能力', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (54, 2283, '软件设计师', '中级', '工信部', '2025-03-20', NULL, 'SD202503200456', '软件设计与架构能力认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (55, 2283, 'MySQL数据库管理员', '中级', 'MySQL官方', '2025-01-10', '2028-01-10', 'DB202501100789', '数据库设计与管理专业证书', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);
INSERT INTO `user_certificate` VALUES (56, 2283, '阿里云开发者', '专业级', '阿里云', '2024-12-05', '2027-12-05', 'AL20241205025', '云原生应用开发认证', NULL, NULL, '2025-09-18 13:29:26', NULL, '2025-09-18 13:29:26', NULL);

-- ----------------------------
-- Table structure for user_honors
-- ----------------------------
DROP TABLE IF EXISTS `user_honors`;
CREATE TABLE `user_honors`  (
  `honors_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `honor_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '荣誉名称',
  `honor_level` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '荣誉级别(校级/省级/国家级等)',
  `issue_org` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '颁发机构',
  `issue_date` date NULL DEFAULT NULL COMMENT '获得日期',
  `certificate_no` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '证书编号',
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '详细描述',
  `attachment` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '证书附件路径',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `create_user` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `update_user` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`honors_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `user_honors_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_honors
-- ----------------------------
INSERT INTO `user_honors` VALUES (34, 1, '123', '市级', 'adfwe', '2025-07-18', NULL, '12312312', 'user/honor/2025/07/13/628b8db8038e476f98aa81c22d1a842d.png', '2025-07-13 16:01:55', NULL, '2025-07-13 16:01:55', NULL);
INSERT INTO `user_honors` VALUES (36, 2283, '互联网+', '省级', '重庆市教委', '2025-09-17', NULL, '1231', 'user/honor/2025/09/02/ec9830b55fec4ba4910986b360c1d7f0.png', '2025-09-02 16:29:43', NULL, '2025-09-02 16:29:43', NULL);
INSERT INTO `user_honors` VALUES (37, 1, '优秀学员', '校级', '重庆机电职业技术大学', '2024-12-10', 'XY20241210001', '学习成绩优异，表现突出', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (38, 1, '编程竞赛三等奖', '市级', '重庆市教委', '2024-10-15', 'BC20241015002', 'Python编程大赛获奖', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (39, 2255, '优秀指导教师', '省级', '重庆市教育厅', '2024-06-01', 'ZD20240601003', '指导学生获得省级竞赛奖项', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (40, 2255, '教学创新奖', '校级', '重庆机电职业技术大学', '2024-09-10', 'CX20240910004', '教学方法创新，效果显著', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (41, 2255, '技术专家', '行业级', '中国软件行业协会', '2023-12-20', 'ZJ20231220005', '在软件开发领域贡献突出', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (42, 2271, 'Web开发竞赛二等奖', '市级', '重庆市科协', '2024-11-20', 'WB20241120006', '前端开发技术优秀', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (43, 2272, '数学建模竞赛优秀奖', '省级', '重庆市教委', '2024-05-25', 'SX20240525007', '数学建模应用能力强', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (44, 2272, '学习进步奖', '校级', '重庆机电职业技术大学', '2024-12-15', 'JB20241215008', '学习态度端正，进步显著', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (45, 2273, '设计创新大赛一等奖', '省级', '重庆市教育厅', '2024-08-30', 'SJ20240830009', '创意设计作品获得一等奖', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (46, 2273, '优秀团员', '校级', '重庆机电职业技术大学团委', '2024-05-04', 'TY20240504010', '积极参与团组织活动', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (47, 2274, 'ACM程序设计竞赛优秀奖', '区域级', 'ACM-ICPC', '2024-04-15', 'AC20240415011', '算法设计能力突出', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (48, 2274, '创新创业大赛三等奖', '校级', '重庆机电职业技术大学', '2024-10-08', 'CY20241008012', '创新项目设计优秀', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (49, 2275, '网络安全技能大赛二等奖', '省级', '重庆市教委', '2024-07-12', 'WL20240712013', '网络安全防护技能优秀', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (50, 2275, '优秀志愿者', '市级', '重庆市志愿者协会', '2024-12-05', 'ZY20241205014', '积极参与社会志愿服务', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (51, 2276, '人工智能挑战赛入围奖', '国家级', '教育部', '2024-09-18', 'AI20240918015', 'AI算法应用创新', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (52, 2277, '区块链应用开发竞赛优胜奖', '省级', '重庆市科技厅', '2024-11-25', 'QL20241125016', '区块链技术应用突出', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (53, 2277, '科技创新奖学金', '校级', '重庆机电职业技术大学', '2024-12-20', 'KJ20241220017', '科研创新能力优秀', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (54, 2278, '大数据分析竞赛一等奖', '市级', '重庆市大数据局', '2024-06-18', 'DS20240618018', '大数据处理分析能力强', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (55, 2278, '优秀班干部', '校级', '重庆机电职业技术大学', '2024-09-01', 'BG20240901019', '班级管理工作出色', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (56, 2279, '系统运维技能大赛三等奖', '省级', '重庆市教委', '2024-08-22', 'YW20240822020', '运维技能扎实', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (57, 2281, '游戏开发创意大赛优秀奖', '校级', '重庆机电职业技术大学', '2024-12-10', 'YX20241210021', '游戏创意设计新颖', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (58, 2282, '管理创新奖', '省级', '重庆市人社局', '2023-11-15', 'GL20231115022', '管理模式创新，成效显著', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (59, 2282, '优秀管理者', '校级', '重庆机电职业技术大学', '2024-01-20', 'YX20240120023', '管理工作表现突出', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (60, 2282, '信息化建设先进个人', '市级', '重庆市经信委', '2023-09-30', 'XX20230930024', '推进信息化建设贡献突出', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (61, 2283, '软件设计大赛一等奖', '校级', '重庆机电职业技术大学', '2024-11-12', 'RJ20241112025', '软件架构设计优秀', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (62, 2283, '编程马拉松优胜奖', '市级', '重庆市软件协会', '2024-08-15', 'BM20240815026', '编程能力突出，解决方案创新', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (63, 2283, '优秀实习生', '企业级', '某知名互联网公司', '2024-07-30', 'SX20240730027', '实习期间表现优异，技能掌握快', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);
INSERT INTO `user_honors` VALUES (64, 2283, '技术分享达人', '校级', '重庆机电职业技术大学', '2025-01-05', 'JS20250105028', '积极分享技术经验，帮助同学进步', NULL, '2025-09-18 13:29:36', NULL, '2025-09-18 13:29:36', NULL);

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role`  (
  `user_id` bigint UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `is_current` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否激活',
  `assigned_by` bigint UNSIGNED NULL DEFAULT NULL COMMENT '分配人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE,
  UNIQUE INDEX `uk_current_role`(`user_id` ASC, `is_current` ASC) USING BTREE,
  INDEX `user_role_ibfk_2`(`role_id` ASC) USING BTREE,
  INDEX `user_role_ibfk_3`(`assigned_by` ASC) USING BTREE,
  CONSTRAINT `user_role_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_role_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_role_ibfk_3` FOREIGN KEY (`assigned_by`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_role
-- ----------------------------

-- ----------------------------
-- Table structure for visitor
-- ----------------------------
DROP TABLE IF EXISTS `visitor`;
CREATE TABLE `visitor`  (
  `visitor_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint UNSIGNED NULL DEFAULT NULL,
  `user_name` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `password` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `name` varchar(10) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`visitor_id`) USING BTREE,
  INDEX `fk_visitor_user`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_visitor_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of visitor
-- ----------------------------
INSERT INTO `visitor` VALUES (3, NULL, 'test', '123456', '测试', '2025-06-07 00:43:42');

-- ----------------------------
-- View structure for v_active_sessions
-- ----------------------------
DROP VIEW IF EXISTS `v_active_sessions`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_active_sessions` AS select `s`.`session_id` AS `session_id`,`s`.`user_id` AS `user_id`,`u`.`name` AS `user_name`,`s`.`current_model` AS `current_model`,`s`.`current_question_type` AS `current_question_type`,`s`.`turn_count` AS `turn_count`,`s`.`model_locked` AS `model_locked`,`s`.`lock_reason` AS `lock_reason`,`s`.`last_interaction` AS `last_interaction`,timestampdiff(MINUTE,`s`.`last_interaction`,now()) AS `minutes_since_last_interaction` from (`ai_session_state` `s` left join `user` `u` on((`s`.`user_id` = `u`.`user_id`))) where (`s`.`last_interaction` > (now() - interval 30 minute)) order by `s`.`last_interaction` desc;

-- ----------------------------
-- View structure for v_course_students
-- ----------------------------
DROP VIEW IF EXISTS `v_course_students`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_course_students` AS select `c`.`course_id` AS `course_id`,`c`.`name` AS `course_name`,`s`.`student_id` AS `student_id`,`u`.`name` AS `student_name`,`s`.`majorClass` AS `student_major`,`td`.`direction_name` AS `training_direction` from ((((`course` `c` left join `student_course` `sc` on((`c`.`course_id` = `sc`.`course_id`))) left join `student` `s` on((`sc`.`student_id` = `s`.`student_id`))) left join `user` `u` on((`s`.`user_id` = `u`.`user_id`))) left join `training_direction` `td` on((`s`.`direction_id` = `td`.`direction_id`)));

-- ----------------------------
-- View structure for v_model_usage_summary
-- ----------------------------
DROP VIEW IF EXISTS `v_model_usage_summary`;
CREATE ALGORITHM = UNDEFINED SQL SECURITY DEFINER VIEW `v_model_usage_summary` AS select `ai_model_usage_stats`.`model_name` AS `model_name`,`ai_model_usage_stats`.`question_type` AS `question_type`,`ai_model_usage_stats`.`usage_count` AS `usage_count`,`ai_model_usage_stats`.`total_turns` AS `total_turns`,round((`ai_model_usage_stats`.`total_turns` / `ai_model_usage_stats`.`usage_count`),2) AS `avg_turns_per_session`,`ai_model_usage_stats`.`success_rate` AS `success_rate`,`ai_model_usage_stats`.`avg_response_time` AS `avg_response_time`,`ai_model_usage_stats`.`date_created` AS `date_created` from `ai_model_usage_stats` order by `ai_model_usage_stats`.`date_created` desc,`ai_model_usage_stats`.`usage_count` desc;

-- ----------------------------
-- Procedure structure for generate_attendance_statistics
-- ----------------------------
DROP PROCEDURE IF EXISTS `generate_attendance_statistics`;
delimiter ;;
CREATE PROCEDURE `generate_attendance_statistics`(IN stat_date DATE)
BEGIN
    -- 活动考勤统计
    INSERT INTO attendance_statistics (type, date, total_count, present_count, late_count, absent_count, leave_count)
    SELECT 
        'activity' as type,
        stat_date as date,
        COUNT(*) as total_count,
        SUM(CASE WHEN ar.status = 'present' THEN 1 ELSE 0 END) as present_count,
        SUM(CASE WHEN ar.status = 'late' THEN 1 ELSE 0 END) as late_count,
        SUM(CASE WHEN ar.status = 'absent' THEN 1 ELSE 0 END) as absent_count,
        SUM(CASE WHEN ar.status = 'leave' THEN 1 ELSE 0 END) as leave_count
    FROM attendance_record ar
    JOIN attendance_plan ap ON ar.plan_id = ap.plan_id
    WHERE ap.type = 'activity' 
    AND DATE(ap.start_time) = stat_date
    ON DUPLICATE KEY UPDATE
        total_count = VALUES(total_count),
        present_count = VALUES(present_count),
        late_count = VALUES(late_count),
        absent_count = VALUES(absent_count),
        leave_count = VALUES(leave_count);
    
    -- 课程考勤统计
    INSERT INTO attendance_statistics (type, date, total_count, present_count, late_count, absent_count, leave_count)
    SELECT 
        'course' as type,
        stat_date as date,
        COUNT(*) as total_count,
        SUM(CASE WHEN ar.status = 'present' THEN 1 ELSE 0 END) as present_count,
        SUM(CASE WHEN ar.status = 'late' THEN 1 ELSE 0 END) as late_count,
        SUM(CASE WHEN ar.status = 'absent' THEN 1 ELSE 0 END) as absent_count,
        SUM(CASE WHEN ar.status = 'leave' THEN 1 ELSE 0 END) as leave_count
    FROM attendance_record ar
    JOIN attendance_plan ap ON ar.plan_id = ap.plan_id
    WHERE ap.type = 'course' 
    AND DATE(ap.start_time) = stat_date
    ON DUPLICATE KEY UPDATE
        total_count = VALUES(total_count),
        present_count = VALUES(present_count),
        late_count = VALUES(late_count),
        absent_count = VALUES(absent_count),
        leave_count = VALUES(leave_count);
    
    -- 值班考勤统计
    INSERT INTO attendance_statistics (type, date, total_count, present_count, late_count, absent_count, leave_count)
    SELECT 
        'duty' as type,
        stat_date as date,
        COUNT(*) as total_count,
        SUM(CASE WHEN ar.status = 'present' THEN 1 ELSE 0 END) as present_count,
        SUM(CASE WHEN ar.status = 'late' THEN 1 ELSE 0 END) as late_count,
        SUM(CASE WHEN ar.status = 'absent' THEN 1 ELSE 0 END) as absent_count,
        SUM(CASE WHEN ar.status = 'leave' THEN 1 ELSE 0 END) as leave_count
    FROM attendance_record ar
    JOIN attendance_plan ap ON ar.plan_id = ap.plan_id
    WHERE ap.type = 'duty' 
    AND DATE(ap.start_time) = stat_date
    ON DUPLICATE KEY UPDATE
        total_count = VALUES(total_count),
        present_count = VALUES(present_count),
        late_count = VALUES(late_count),
        absent_count = VALUES(absent_count),
        leave_count = VALUES(leave_count);
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
