SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `book`;
CREATE TABLE `book`  (
 `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT 'id',
 `isbn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图书编号(ISBN/内部编号)',
 `barcode` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '条形码/二维码内容(扫码匹配字段)',
 `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
 `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '价格',
 `author` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '作者',
 `publisher` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出版社',
 `create_time` date NULL DEFAULT NULL COMMENT '出版时间',
 `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '0：不可借(库存为0) 1：可借(库存>0)',
 `borrownum` int(0) NOT NULL COMMENT '此书被借阅次数',
 `stock` int(0) NOT NULL DEFAULT 1 COMMENT '库存数量(可借数量)',
 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

INSERT INTO `book` VALUES
(1, '12341541321',   '12341541321',   '数据结构', 15.00, '小王', '宁波大学出版社', '2025-12-16', '1', 0, 1),
(2, '2312315132131', '2312315132131', '机器学习基础', NULL,  NULL,  NULL,           NULL,        '1', 0, 1),
(3, '25213121232',   '25213121232',   'JAVA WEB开发基础', NULL,  NULL,  NULL,           NULL,        '1', 0, 1),
(4, '3213123123',    '3213123123',    '操作系统',     NULL,  NULL,  NULL,           NULL,        '1', 0, 0),
(5, '345621212321',  '345621212321',  '伊索寓言',     NULL,  NULL,  NULL,           NULL,        '1', 0, 0),
(6, '54112312321',   '54112312321',   '格林童话',     NULL,  NULL,  NULL,           NULL,        '1', 0, 1),
(7, '54112312323',   'http://weixin.qq.com/r/tzkZAXHENyxIrTVi92yl',   '运筹学',     NULL,  NULL,  NULL,           NULL,        '1', 0, 1);

DROP TABLE IF EXISTS `bookwithuser`;
CREATE TABLE `bookwithuser`  (
 `id` bigint(0) NOT NULL COMMENT '读者id',
 `isbn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图书编号',
 `book_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图书名',
 `nick_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '读者姓名',
 `lendtime` datetime(0) NULL DEFAULT NULL COMMENT '借阅时间',
 `deadtime` datetime(0) NULL DEFAULT NULL COMMENT '应归还时间',
 `prolong` int(0) NULL DEFAULT NULL COMMENT '续借次数',
 PRIMARY KEY (`id`, `isbn`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `lend_record`;
CREATE TABLE `lend_record`  (
  `reader_id` bigint(0) NOT NULL COMMENT '读者id',
  `isbn` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图书编号',
  `bookname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图书名',
  `lend_time` datetime(0) NULL DEFAULT NULL COMMENT '借书日期',
  `return_time` datetime(0) NULL DEFAULT NULL COMMENT '还书日期',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '0：未归还 1：已归还',
  `borrownum` int(0) NOT NULL COMMENT '此书被借阅次数'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `nick_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '姓名',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话号码',
  `sex` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '性别',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `role` int(0) NOT NULL COMMENT '角色、1：管理员 2：普通用户',
  `face_descriptor` JSON NULL COMMENT '人脸特征向量(128维 float数组)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

INSERT INTO `user` VALUES
(1, 'admin', '111', '卢家珺', NULL, NULL, NULL, 1, NULL),
(2, 'user1', '111', '陈姜胜', NULL, NULL, NULL, 2, NULL),
(3, 'user2', '111', '周星宇', NULL, NULL, NULL, 2, NULL),
(4, 'user3', '111', '刁翔宇', NULL, NULL, NULL, 2, NULL),
(5, 'user4', '111', '王皓民', NULL, NULL, NULL, 2, NULL);