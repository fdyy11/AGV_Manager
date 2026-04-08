SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` int(10) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '角色标识',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES (1, 'admin', 'admin', '管理员', 'http://localhost:9090/files/1697438073596-avatar.png','ADMIN', '13677889922', 'admin@xm.com');

-- ----------------------------
-- Table structure for notice
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内容',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `user` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '公告信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of notice
-- ----------------------------
INSERT INTO `notice` VALUES (1, '今天系统正式上线，开始内测', '今天系统正式上线，开始内测', '2023-09-05', 'admin');
INSERT INTO `notice` VALUES (2, '所有功能都已完成，可以正常使用', '所有功能都已完成，可以正常使用', '2023-09-05', 'admin');
INSERT INTO `notice` VALUES (3, '今天天气很不错，可以出去一起玩了', '今天天气很不错，可以出去一起玩了', '2023-09-05', 'admin');

-- ----------------------------
-- Table structure for map_node
-- ----------------------------
DROP TABLE IF EXISTS `map_node`;
CREATE TABLE `map_node`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '节点 ID',
  `x` double NULL DEFAULT NULL COMMENT 'X 坐标',
  `y` double NULL DEFAULT NULL COMMENT 'Y 坐标',
  `node_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '节点类型（storage:普通节点，assembly:站点，charging:充电站，intersection:路口）',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `capacity` int(11) NULL DEFAULT NULL COMMENT '容量',
  `is_available` tinyint(1) NULL DEFAULT 1 COMMENT '是否可用',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_node_id`(`node_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '地图节点表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of map_node (示例数据)
-- ----------------------------
INSERT INTO `map_node` VALUES (1, 'N001', 100.0, 100.0, 'storage', '普通存储节点', 10, 1);
INSERT INTO `map_node` VALUES (2, 'N002', 200.0, 100.0, 'assembly', '装配站点', 5, 1);
INSERT INTO `map_node` VALUES (3, 'N003', 300.0, 100.0, 'charging', '充电站', 2, 1);
INSERT INTO `map_node` VALUES (4, 'N004', 150.0, 200.0, 'intersection', '交叉路口', 20, 1);
INSERT INTO `map_node` VALUES (5, 'N005', 250.0, 200.0, 'storage', '普通存储节点', 15, 1);

-- ----------------------------
-- Table structure for map_edge
-- ----------------------------
DROP TABLE IF EXISTS `map_edge`;
CREATE TABLE `map_edge`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `from_node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '起始节点 ID',
  `to_node_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标节点 ID',
  `weight` double NULL DEFAULT NULL COMMENT '权重（距离）',
  `edge_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '边类型',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_from_node`(`from_node_id`) USING BTREE,
  INDEX `idx_to_node`(`to_node_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '地图边表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of map_edge (示例数据)
-- ----------------------------
INSERT INTO `map_edge` VALUES (1, 'N001', 'N002', 100.0, 'normal');
INSERT INTO `map_edge` VALUES (2, 'N002', 'N003', 100.0, 'normal');
INSERT INTO `map_edge` VALUES (3, 'N001', 'N004', 111.8, 'normal');
INSERT INTO `map_edge` VALUES (4, 'N002', 'N005', 111.8, 'normal');
INSERT INTO `map_edge` VALUES (5, 'N004', 'N005', 100.0, 'normal');

-- ----------------------------
-- Table structure for agv
-- ----------------------------
DROP TABLE IF EXISTS `agv`;
CREATE TABLE `agv`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `agv_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AGV 编号',
  `current_location` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前位置（节点 ID）',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态（idle, working, charging, fault）',
  `battery_level` int(11) NULL DEFAULT NULL COMMENT '电量百分比',
  `carrying_material` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '承载物料',
  `assigned_task` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '当前任务',
  `speed` double NULL DEFAULT NULL COMMENT '当前速度',
  `last_update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `ip_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'IP 地址',
  `port` int(11) NULL DEFAULT NULL COMMENT '端口号',
  `mac_address` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'MAC 地址',
  `is_online` tinyint(1) NULL DEFAULT 0 COMMENT '是否在线',
  `current_x` double NULL DEFAULT NULL COMMENT '当前 X 坐标（单位：m，从 AGV API 获取的实时坐标）',
  `current_y` double NULL DEFAULT NULL COMMENT '当前 Y 坐标（单位：m，从 AGV API 获取的实时坐标）',
  `current_theta` double NULL DEFAULT NULL COMMENT '当前角度（单位：rad，从 AGV API 获取的实时坐标）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_agv_id`(`agv_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_is_online`(`is_online`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AGV 信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of agv (示例数据，实际使用时删除)
-- ----------------------------
INSERT INTO `agv` VALUES (1, 'AGV_001', 'N001', 'idle', 95, NULL, NULL, 1.0, NOW(), '127.0.0.1', 5555, NULL, 1, 50.0, 50.0, 0.5);
INSERT INTO `agv` VALUES (2, 'AGV_002', 'N002', 'working', 80, '物料 A', 'TASK_001', 1.2, NOW(), '127.0.0.1', 5556, NULL, 1, 200.0, 100.0, 0.0);

SET FOREIGN_KEY_CHECKS = 1;
