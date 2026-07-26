-- MySQL 8.x schema derived from the current JPA entities.
-- Spring Boot's default CamelCaseToUnderscoresNamingStrategy is assumed.
-- Store and read all Instant values in UTC.

CREATE TABLE IF NOT EXISTS `sign_info` (
    `user_num` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(60) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `deleted_at` DATETIME(6) NULL,
    `last_login` DATETIME(6) NOT NULL,
    PRIMARY KEY (`user_num`),
    CONSTRAINT `uk_sign_info_email` UNIQUE (`email`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_info` (
    `profile_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_num` BIGINT NOT NULL,
    `nickname` VARCHAR(10) NOT NULL,
    `profile_image` VARCHAR(255) NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `deleted_at` DATETIME(6) NULL,
    PRIMARY KEY (`profile_id`),
    CONSTRAINT `uk_user_info_user_num` UNIQUE (`user_num`),
    CONSTRAINT `uk_user_info_nickname` UNIQUE (`nickname`),
    CONSTRAINT `ck_user_info_role` CHECK (`role` IN ('ADMIN', 'USER')),
    CONSTRAINT `fk_user_info_sign_info`
        FOREIGN KEY (`user_num`) REFERENCES `sign_info` (`user_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post` (
    `post_num` BIGINT NOT NULL AUTO_INCREMENT,
    `profile_id` BIGINT NOT NULL,
    `title` VARCHAR(26) NOT NULL,
    `content` TEXT NOT NULL,
    `image` VARCHAR(255) NULL,
    `deleted_at` DATETIME(6) NULL,
    `edited_at` DATETIME(6) NULL,
    `write_at` DATETIME(6) NOT NULL,
    `version` INT NOT NULL DEFAULT 1,
    PRIMARY KEY (`post_num`),
    KEY `idx_post_profile_id` (`profile_id`),
    CONSTRAINT `fk_post_user_info`
        FOREIGN KEY (`profile_id`) REFERENCES `user_info` (`profile_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_stat` (
    `post_num` BIGINT NOT NULL,
    `view_count` INT NOT NULL DEFAULT 0,
    `like_count` INT NOT NULL DEFAULT 0,
    `comment_count` INT NOT NULL DEFAULT 0,
    `report_count` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`post_num`),
    CONSTRAINT `fk_post_stat_post`
        FOREIGN KEY (`post_num`) REFERENCES `post` (`post_num`)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `ck_post_stat_view_count` CHECK (`view_count` >= 0),
    CONSTRAINT `ck_post_stat_like_count` CHECK (`like_count` >= 0),
    CONSTRAINT `ck_post_stat_comment_count` CHECK (`comment_count` >= 0),
    CONSTRAINT `ck_post_stat_report_count` CHECK (`report_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `temporary_post` (
    `temporary_id` BIGINT NOT NULL AUTO_INCREMENT,
    `profile_id` BIGINT NOT NULL,
    `title` VARCHAR(26) NULL,
    `content` TEXT NULL,
    `image` VARCHAR(255) NULL,
    `write_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`temporary_id`),
    KEY `idx_temporary_post_profile_id` (`profile_id`),
    CONSTRAINT `fk_temporary_post_user_info`
        FOREIGN KEY (`profile_id`) REFERENCES `user_info` (`profile_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_like_post` (
    `like_id` BIGINT NOT NULL AUTO_INCREMENT,
    `profile_id` BIGINT NOT NULL,
    `post_num` BIGINT NOT NULL,
    PRIMARY KEY (`like_id`),
    CONSTRAINT `uk_user_like_post_profile_post` UNIQUE (`profile_id`, `post_num`),
    KEY `idx_user_like_post_post_num` (`post_num`),
    CONSTRAINT `fk_user_like_post_user_info`
        FOREIGN KEY (`profile_id`) REFERENCES `user_info` (`profile_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_user_like_post_post`
        FOREIGN KEY (`post_num`) REFERENCES `post` (`post_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_view` (
    `post_view_id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_num` BIGINT NOT NULL,
    `profile_id` BIGINT NOT NULL,
    `view_at` DATETIME(6) NULL,
    PRIMARY KEY (`post_view_id`),
    CONSTRAINT `uk_post_view_post_profile` UNIQUE (`post_num`, `profile_id`),
    KEY `idx_post_view_profile_id` (`profile_id`),
    KEY `idx_post_view_time_post` (`view_at`, `post_num`),
    CONSTRAINT `fk_post_view_post`
        FOREIGN KEY (`post_num`) REFERENCES `post` (`post_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_post_view_user_info`
        FOREIGN KEY (`profile_id`) REFERENCES `user_info` (`profile_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_report` (
    `report_id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_num` BIGINT NOT NULL,
    `profile_id` BIGINT NOT NULL,
    PRIMARY KEY (`report_id`),
    CONSTRAINT `uk_post_report_post_profile` UNIQUE (`post_num`, `profile_id`),
    KEY `idx_post_report_profile_id` (`profile_id`),
    CONSTRAINT `fk_post_report_post`
        FOREIGN KEY (`post_num`) REFERENCES `post` (`post_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_post_report_user_info`
        FOREIGN KEY (`profile_id`) REFERENCES `user_info` (`profile_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `post_edit_record` (
    `post_edit_id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_num` BIGINT NOT NULL,
    `version` INT NOT NULL,
    `title` VARCHAR(26) NOT NULL,
    `content` TEXT NOT NULL,
    `image` VARCHAR(255) NULL,
    `write_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`post_edit_id`),
    CONSTRAINT `uk_post_edit_record_post_version` UNIQUE (`post_num`, `version`),
    CONSTRAINT `fk_post_edit_record_post`
        FOREIGN KEY (`post_num`) REFERENCES `post` (`post_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment` (
    `comment_num` BIGINT NOT NULL AUTO_INCREMENT,
    `post_num` BIGINT NOT NULL,
    `parent_num` BIGINT NULL,
    `depth` INT NOT NULL DEFAULT 0,
    `profile_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `deleted_at` DATETIME(6) NULL,
    `edited_at` DATETIME(6) NULL,
    `write_at` DATETIME(6) NOT NULL,
    `version` INT NOT NULL DEFAULT 0,
    `child_count` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`comment_num`),
    KEY `idx_comment_post_depth` (`post_num`, `depth`),
    KEY `idx_comment_parent_num` (`parent_num`),
    KEY `idx_comment_profile_id` (`profile_id`),
    CONSTRAINT `fk_comment_post`
        FOREIGN KEY (`post_num`) REFERENCES `post` (`post_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_comment_parent`
        FOREIGN KEY (`parent_num`) REFERENCES `comment` (`comment_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `fk_comment_user_info`
        FOREIGN KEY (`profile_id`) REFERENCES `user_info` (`profile_id`)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT `ck_comment_depth` CHECK (`depth` BETWEEN 0 AND 3),
    CONSTRAINT `ck_comment_child_count` CHECK (`child_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `comment_edit_record` (
    `comment_edit_id` BIGINT NOT NULL AUTO_INCREMENT,
    `comment_num` BIGINT NULL,
    `version` INT NOT NULL,
    `content` TEXT NOT NULL,
    `write_at` DATETIME(6) NOT NULL,
    PRIMARY KEY (`comment_edit_id`),
    CONSTRAINT `uk_comment_edit_record_comment_version`
        UNIQUE (`comment_num`, `version`),
    CONSTRAINT `fk_comment_edit_record_comment`
        FOREIGN KEY (`comment_num`) REFERENCES `comment` (`comment_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `refresh_token` (
    `refresh_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_num` BIGINT NOT NULL,
    `token` VARCHAR(512) NULL,
    PRIMARY KEY (`refresh_id`),
    KEY `idx_refresh_token_token` (`token`),
    KEY `idx_refresh_token_user_token` (`user_num`, `token`),
    CONSTRAINT `fk_refresh_token_sign_info`
        FOREIGN KEY (`user_num`) REFERENCES `sign_info` (`user_num`)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
