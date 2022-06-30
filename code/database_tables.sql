/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 80015
 Source Host           : localhost:3306
 Source Schema         : vldb_open_source

 Target Server Type    : MySQL
 Target Server Version : 80015
 File Encoding         : 65001

 Date: 30/06/2022 15:08:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for dataset_summary
-- ----------------------------
DROP TABLE IF EXISTS `dataset_summary`;
CREATE TABLE `dataset_summary`  (
  `dataset_id` int(10) NULL DEFAULT NULL,
  `file_id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `name` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `author` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
  `url` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `download` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `other_url` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `created` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `updated` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `license` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `dataset_id_ckan` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `parent_fxf` text CHARACTER SET utf8 COLLATE utf8_bin NULL,
  `version` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `size` varchar(30) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `source` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `id` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `db_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL DEFAULT NULL,
  `stored` tinyint(4) NULL DEFAULT NULL,
  PRIMARY KEY (`file_id`) USING BTREE,
  INDEX `dataset_id`(`dataset_id`) USING BTREE,
  INDEX `if_stored`(`stored`) USING BTREE,
  INDEX `dataset_summary`(`dataset_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 326113 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for rdf_term
-- ----------------------------
DROP TABLE IF EXISTS `rdf_term`;
CREATE TABLE `rdf_term`  (
  `file_id` int(11) NOT NULL,
  `iri` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `label` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL,
  `kind` int(11) NOT NULL,
  `id` int(11) NOT NULL,
  INDEX `resource_id`(`file_id`) USING BTREE,
  INDEX `resource_id, id`(`file_id`, `id`) USING BTREE,
  INDEX `resource_id, kind`(`file_id`, `kind`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Compact;

-- ----------------------------
-- Table structure for triple
-- ----------------------------
DROP TABLE IF EXISTS `triple`;
CREATE TABLE `triple`  (
  `file_id` int(255) NOT NULL,
  `msg_code` binary(16) NULL DEFAULT NULL,
  `subject` int(255) NOT NULL,
  `predicate` int(255) NOT NULL,
  `object` int(255) NOT NULL,
  INDEX `resource_id`(`file_id`) USING BTREE,
  INDEX `resource_id, S, P, O`(`file_id`, `subject`, `predicate`, `object`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;
