-- =========================================================
-- Community MySQL 8.x schema reset script
--
-- 주의:
--   아래 DROP TABLE은 기존 데이터를 모두 삭제합니다.
--
-- 전제:
--   1. Spring Boot CamelCaseToUnderscoresNamingStrategy 사용
--   2. post.version/comment.version은 JPA @Version 낙관적 락 컬럼
--   3. Instant 값은 UTC 기준으로 저장/조회
--   4. 현재는 계정당 프로필 1개
-- =========================================================


-- =========================================================
-- 1. 기존 테이블 제거
-- 외래 키를 가진 자식 테이블부터 제거합니다.
-- =========================================================

DROP TABLE IF EXISTS `refresh_token`;
DROP TABLE IF EXISTS `comment_edit_record`;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `post_edit_record`;
DROP TABLE IF EXISTS `post_report`;
DROP TABLE IF EXISTS `popularity_aggregation_checkpoint`;
DROP TABLE IF EXISTS `post_popularity_stat`;
DROP TABLE IF EXISTS `post_view_bucket`;
DROP TABLE IF EXISTS `post_view`;
DROP TABLE IF EXISTS `user_like_post`;
DROP TABLE IF EXISTS `temporary_post`;
DROP TABLE IF EXISTS `post_stat`;
DROP TABLE IF EXISTS `post`;
DROP TABLE IF EXISTS `user_info`;
DROP TABLE IF EXISTS `sign_info`;


-- =========================================================
-- 2. 회원 인증 정보
-- =========================================================

CREATE TABLE `sign_info` (
                             `user_num` BIGINT NOT NULL AUTO_INCREMENT,
                             `email` VARCHAR(60) NOT NULL,
                             `password` VARCHAR(255) NOT NULL,
                             `deleted_at` DATETIME(6) NULL,
                             `last_login` DATETIME(6) NULL,

                             PRIMARY KEY (`user_num`),

                             CONSTRAINT `uk_sign_info_email`
                                 UNIQUE (`email`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 3. 사용자 프로필
--
-- user_num UNIQUE:
-- 현재는 계정 하나당 프로필 하나만 허용합니다.
-- 다중 프로필로 변경할 경우 이 UNIQUE 제약조건을 제거해야 합니다.
-- =========================================================

CREATE TABLE `user_info` (
                             `profile_id` BIGINT NOT NULL AUTO_INCREMENT,
                             `user_num` BIGINT NOT NULL,
                             `nickname` VARCHAR(10) NOT NULL,
                             `profile_image` VARCHAR(255) NULL,
                             `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
                             `deleted_at` DATETIME(6) NULL,

                             PRIMARY KEY (`profile_id`),

                             CONSTRAINT `uk_user_info_user_num`
                                 UNIQUE (`user_num`),

                             CONSTRAINT `uk_user_info_nickname`
                                 UNIQUE (`nickname`),

                             CONSTRAINT `ck_user_info_role`
                                 CHECK (`role` IN ('ADMIN', 'USER')),

                             CONSTRAINT `fk_user_info_sign_info`
                                 FOREIGN KEY (`user_num`)
                                     REFERENCES `sign_info` (`user_num`)
                                     ON UPDATE RESTRICT
                                     ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 4. 게시글
--
-- version:
-- JPA @Version 낙관적 락 컬럼이므로 0부터 시작합니다.
-- Hibernate가 INSERT 시 직접 0을 넣으면 DB DEFAULT는 사용되지
-- 않을 수 있지만, 스키마 의미도 0으로 맞춥니다.
-- =========================================================

CREATE TABLE `post` (
                        `post_num` BIGINT NOT NULL AUTO_INCREMENT,
                        `profile_id` BIGINT NOT NULL,
                        `title` VARCHAR(26) NOT NULL,
                        `content` TEXT NOT NULL,
                        `image` VARCHAR(255) NULL,
                        `deleted_at` DATETIME(6) NULL,
                        `edited_at` DATETIME(6) NULL,
                        `write_at` DATETIME(6) NOT NULL,
                        `version` INT NOT NULL DEFAULT 0,

                        PRIMARY KEY (`post_num`),

                        KEY `idx_post_profile_id` (`profile_id`),
                        KEY `idx_post_write_at` (`write_at`),
                        KEY `idx_post_deleted_write_at` (`deleted_at`, `write_at`),

                        CONSTRAINT `fk_post_user_info`
                            FOREIGN KEY (`profile_id`)
                                REFERENCES `user_info` (`profile_id`)
                                ON UPDATE RESTRICT
                                ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 5. 게시글 통계
--
-- post를 생성할 때 post_stat도 애플리케이션에서 함께 생성해야 합니다.
-- 외래 키만으로는 post_stat 행이 자동 생성되지 않습니다.
-- =========================================================

CREATE TABLE `post_stat` (
                             `post_num` BIGINT NOT NULL,
                             `view_count` BIGINT NOT NULL DEFAULT 0,
                             `like_count` BIGINT NOT NULL DEFAULT 0,
                             `comment_count` BIGINT NOT NULL DEFAULT 0,
                             `report_count` BIGINT NOT NULL DEFAULT 0,

                             PRIMARY KEY (`post_num`),

                             CONSTRAINT `fk_post_stat_post`
                                 FOREIGN KEY (`post_num`)
                                     REFERENCES `post` (`post_num`)
                                     ON UPDATE RESTRICT
                                     ON DELETE CASCADE,

                             CONSTRAINT `ck_post_stat_view_count`
                                 CHECK (`view_count` >= 0),

                             CONSTRAINT `ck_post_stat_like_count`
                                 CHECK (`like_count` >= 0),

                             CONSTRAINT `ck_post_stat_comment_count`
                                 CHECK (`comment_count` >= 0),

                             CONSTRAINT `ck_post_stat_report_count`
                                 CHECK (`report_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 5-1. 게시글 시간대별 조회수 버킷
--
-- 게시글별 5분 단위 조회수 증가량을 저장합니다.
-- 동일 게시글과 버킷 시작 시각 조합은 하나만 존재합니다.
-- =========================================================

CREATE TABLE `post_view_bucket` (
                                    `post_view_bucket_id` BIGINT NOT NULL AUTO_INCREMENT,
                                    `post_num` BIGINT NOT NULL,
                                    `bucket_start_at` DATETIME(6) NOT NULL,
                                    `view_count` BIGINT NOT NULL DEFAULT 0,

                                    PRIMARY KEY (`post_view_bucket_id`),

                                    CONSTRAINT `uk_post_view_bucket_post_time`
                                        UNIQUE (`post_num`, `bucket_start_at`),

                                    KEY `idx_post_view_bucket_time`
                                        (`bucket_start_at`),

                                    KEY `idx_post_view_bucket_post_time`
                                        (`post_num`, `bucket_start_at`),

                                    CONSTRAINT `fk_post_view_bucket_post`
                                        FOREIGN KEY (`post_num`)
                                            REFERENCES `post` (`post_num`)
                                            ON UPDATE RESTRICT
                                            ON DELETE CASCADE,

                                    CONSTRAINT `ck_post_view_bucket_view_count`
                                        CHECK (`view_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 5-2. 인기글 롤링 집계
-- =========================================================

CREATE TABLE `post_popularity_stat` (
                                        `post_num` BIGINT NOT NULL,
                                        `view_count5m` BIGINT NOT NULL DEFAULT 0,
                                        `view_count30m` BIGINT NOT NULL DEFAULT 0,
                                        `view_count60m` BIGINT NOT NULL DEFAULT 0,
                                        `popularity_score` BIGINT NOT NULL DEFAULT 0,
                                        `window_end_at` DATETIME(6) NOT NULL,

                                        PRIMARY KEY (`post_num`),

                                        KEY `idx_post_popularity_score`
                                            (`popularity_score` DESC,
                                             `view_count5m` DESC,
                                             `view_count30m` DESC,
                                             `post_num` DESC),

                                        CONSTRAINT `fk_post_popularity_stat_post`
                                            FOREIGN KEY (`post_num`)
                                                REFERENCES `post` (`post_num`)
                                                ON UPDATE RESTRICT
                                                ON DELETE CASCADE,

                                        CONSTRAINT `ck_post_popularity_view_count5m`
                                            CHECK (`view_count5m` >= 0),

                                        CONSTRAINT `ck_post_popularity_view_count30m`
                                            CHECK (`view_count30m` >= 0),

                                        CONSTRAINT `ck_post_popularity_view_count60m`
                                            CHECK (`view_count60m` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE `popularity_aggregation_checkpoint` (
                                                     `job_name` VARCHAR(50) NOT NULL,
                                                     `last_processed_end_at` DATETIME(6) NULL,

                                                     PRIMARY KEY (`job_name`)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO `popularity_aggregation_checkpoint` (`job_name`, `last_processed_end_at`)
VALUES ('POPULAR_POSTS', NULL);


-- =========================================================
-- 6. 임시 게시글
-- =========================================================

CREATE TABLE `temporary_post` (
                                  `temporary_id` BIGINT NOT NULL AUTO_INCREMENT,
                                  `profile_id` BIGINT NOT NULL,
                                  `title` VARCHAR(26) NULL,
                                  `content` TEXT NULL,
                                  `image` VARCHAR(255) NULL,
                                  `write_at` DATETIME(6) NOT NULL,

                                  PRIMARY KEY (`temporary_id`),

                                  KEY `idx_temporary_post_profile_id`
                                      (`profile_id`),

                                  KEY `idx_temporary_post_profile_write_at`
                                      (`profile_id`, `write_at`),

                                  CONSTRAINT `fk_temporary_post_user_info`
                                      FOREIGN KEY (`profile_id`)
                                          REFERENCES `user_info` (`profile_id`)
                                          ON UPDATE RESTRICT
                                          ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 7. 게시글 좋아요
-- =========================================================

CREATE TABLE `user_like_post` (
                                  `like_id` BIGINT NOT NULL AUTO_INCREMENT,
                                  `profile_id` BIGINT NOT NULL,
                                  `post_num` BIGINT NOT NULL,

                                  PRIMARY KEY (`like_id`),

                                  CONSTRAINT `uk_user_like_post_profile_post`
                                      UNIQUE (`profile_id`, `post_num`),

                                  KEY `idx_user_like_post_post_num`
                                      (`post_num`),

                                  CONSTRAINT `fk_user_like_post_user_info`
                                      FOREIGN KEY (`profile_id`)
                                          REFERENCES `user_info` (`profile_id`)
                                          ON UPDATE RESTRICT
                                          ON DELETE RESTRICT,

                                  CONSTRAINT `fk_user_like_post_post`
                                      FOREIGN KEY (`post_num`)
                                          REFERENCES `post` (`post_num`)
                                          ON UPDATE RESTRICT
                                          ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 8. 게시글 조회 기록
--
-- 게시글 + 사용자당 한 행만 유지합니다.
-- 24시간 이후 다시 조회하면 view_at을 갱신하는 구조입니다.
-- =========================================================

CREATE TABLE `post_view` (
                             `post_view_id` BIGINT NOT NULL AUTO_INCREMENT,
                             `post_num` BIGINT NOT NULL,
                             `profile_id` BIGINT NOT NULL,
                             `view_at` DATETIME(6) NOT NULL,

                             PRIMARY KEY (`post_view_id`),

                             CONSTRAINT `uk_post_view_post_profile`
                                 UNIQUE (`post_num`, `profile_id`),

                             KEY `idx_post_view_profile_id`
                                 (`profile_id`),

                             KEY `idx_post_view_time_post`
                                 (`view_at`, `post_num`),

                             CONSTRAINT `fk_post_view_post`
                                 FOREIGN KEY (`post_num`)
                                     REFERENCES `post` (`post_num`)
                                     ON UPDATE RESTRICT
                                     ON DELETE RESTRICT,

                             CONSTRAINT `fk_post_view_user_info`
                                 FOREIGN KEY (`profile_id`)
                                     REFERENCES `user_info` (`profile_id`)
                                     ON UPDATE RESTRICT
                                     ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 9. 게시글 신고
-- =========================================================

CREATE TABLE `post_report` (
                               `report_id` BIGINT NOT NULL AUTO_INCREMENT,
                               `post_num` BIGINT NOT NULL,
                               `profile_id` BIGINT NOT NULL,

                               PRIMARY KEY (`report_id`),

                               CONSTRAINT `uk_post_report_post_profile`
                                   UNIQUE (`post_num`, `profile_id`),

                               KEY `idx_post_report_profile_id`
                                   (`profile_id`),

                               CONSTRAINT `fk_post_report_post`
                                   FOREIGN KEY (`post_num`)
                                       REFERENCES `post` (`post_num`)
                                       ON UPDATE RESTRICT
                                       ON DELETE RESTRICT,

                               CONSTRAINT `fk_post_report_user_info`
                                   FOREIGN KEY (`profile_id`)
                                       REFERENCES `user_info` (`profile_id`)
                                       ON UPDATE RESTRICT
                                       ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 10. 게시글 수정 이력
--
-- 이 테이블의 version은 낙관적 락이 아니라 수정본 번호입니다.
-- 애플리케이션에서 1, 2, 3... 형태로 명시적으로 넣습니다.
-- =========================================================

CREATE TABLE `post_edit_record` (
                                    `post_edit_id` BIGINT NOT NULL AUTO_INCREMENT,
                                    `post_num` BIGINT NOT NULL,
                                    `version` INT NOT NULL,
                                    `title` VARCHAR(26) NOT NULL,
                                    `content` TEXT NOT NULL,
                                    `image` VARCHAR(255) NULL,
                                    `write_at` DATETIME(6) NOT NULL,

                                    PRIMARY KEY (`post_edit_id`),

                                    CONSTRAINT `uk_post_edit_record_post_version`
                                        UNIQUE (`post_num`, `version`),

                                    CONSTRAINT `ck_post_edit_record_version`
                                        CHECK (`version` >= 0),

                                    CONSTRAINT `fk_post_edit_record_post`
                                        FOREIGN KEY (`post_num`)
                                            REFERENCES `post` (`post_num`)
                                            ON UPDATE RESTRICT
                                            ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 11. 댓글
--
-- version:
-- JPA @Version 낙관적 락 컬럼이므로 0부터 시작합니다.
-- =========================================================

CREATE TABLE `comment` (
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

                           KEY `idx_comment_post_depth`
                               (`post_num`, `depth`),

                           KEY `idx_comment_post_write_at`
                               (`post_num`, `write_at`),

                           KEY `idx_comment_parent_num`
                               (`parent_num`),

                           KEY `idx_comment_profile_id`
                               (`profile_id`),

                           CONSTRAINT `fk_comment_post`
                               FOREIGN KEY (`post_num`)
                                   REFERENCES `post` (`post_num`)
                                   ON UPDATE RESTRICT
                                   ON DELETE RESTRICT,

                           CONSTRAINT `fk_comment_parent`
                               FOREIGN KEY (`parent_num`)
                                   REFERENCES `comment` (`comment_num`)
                                   ON UPDATE RESTRICT
                                   ON DELETE RESTRICT,

                           CONSTRAINT `fk_comment_user_info`
                               FOREIGN KEY (`profile_id`)
                                   REFERENCES `user_info` (`profile_id`)
                                   ON UPDATE RESTRICT
                                   ON DELETE RESTRICT,

                           CONSTRAINT `ck_comment_depth`
                               CHECK (`depth` BETWEEN 0 AND 3),

                           CONSTRAINT `ck_comment_child_count`
                               CHECK (`child_count` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 12. 댓글 수정 이력
--
-- comment_num은 반드시 댓글을 참조해야 하므로 NOT NULL입니다.
-- 이 테이블의 version은 수정본 번호이므로 1 이상입니다.
-- =========================================================

CREATE TABLE `comment_edit_record` (
                                       `comment_edit_id` BIGINT NOT NULL AUTO_INCREMENT,
                                       `comment_num` BIGINT NOT NULL,
                                       `version` INT NOT NULL,
                                       `content` TEXT NOT NULL,
                                       `write_at` DATETIME(6) NOT NULL,

                                       PRIMARY KEY (`comment_edit_id`),

                                       CONSTRAINT `uk_comment_edit_record_comment_version`
                                           UNIQUE (`comment_num`, `version`),

                                       CONSTRAINT `ck_comment_edit_record_version`
                                           CHECK (`version` >= 0),

                                       CONSTRAINT `fk_comment_edit_record_comment`
                                           FOREIGN KEY (`comment_num`)
                                               REFERENCES `comment` (`comment_num`)
                                               ON UPDATE RESTRICT
                                               ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- =========================================================
-- 13. Refresh Token
--
-- token은 존재하지 않으면 행 자체가 의미 없으므로 NOT NULL입니다.
-- 동일한 토큰이 중복 저장되지 않도록 UNIQUE로 설정합니다.
-- =========================================================

CREATE TABLE `refresh_token` (
                                 `refresh_id` BIGINT NOT NULL AUTO_INCREMENT,
                                 `user_num` BIGINT NOT NULL,
                                 `token` VARCHAR(1024)
                                                  CHARACTER SET ascii
                                     COLLATE ascii_bin
                                                     NOT NULL,

                                 PRIMARY KEY (`refresh_id`),

                                 CONSTRAINT `uk_refresh_token_token`
                                     UNIQUE (`token`),

                                 KEY `idx_refresh_token_user_num`
                                     (`user_num`),

                                 CONSTRAINT `fk_refresh_token_sign_info`
                                     FOREIGN KEY (`user_num`)
                                         REFERENCES `sign_info` (`user_num`)
                                         ON UPDATE RESTRICT
                                         ON DELETE RESTRICT
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
