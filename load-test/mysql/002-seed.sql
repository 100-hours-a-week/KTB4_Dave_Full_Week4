-- Deterministic synthetic dataset for the disposable load-test database.
-- Password for every account: Loadtest1!

SET time_zone = '+00:00';

CREATE TEMPORARY TABLE perf_digit (
    n TINYINT NOT NULL PRIMARY KEY
);

INSERT INTO perf_digit (n)
VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

CREATE TEMPORARY TABLE perf_sequence (
    n INT NOT NULL PRIMARY KEY
);

INSERT INTO perf_sequence (n)
SELECT number_value + 1
FROM (
    SELECT d0.n
         + d1.n * 10
         + d2.n * 100
         + d3.n * 1000
         + d4.n * 10000 AS number_value
    FROM perf_digit d0
    CROSS JOIN perf_digit d1
    CROSS JOIN perf_digit d2
    CROSS JOIN perf_digit d3
    CROSS JOIN perf_digit d4
) numbers
WHERE number_value < 50000;

INSERT INTO sign_info (
    user_num, email, password, deleted_at, last_login
)
SELECT n,
       CONCAT('lt', LPAD(n, 7, '0'), '@load.test'),
       '$2a$10$fIQkprr8O41WBSFimqaY8.RuDW98/Z7AEpvYFVTWOat8Qva9BgHxe',
       NULL,
       NULL
FROM perf_sequence
WHERE n <= 1000;

INSERT INTO user_info (
    profile_id, user_num, nickname, profile_image, role, deleted_at
)
SELECT n,
       n,
       CONCAT('lt', LPAD(n, 7, '0')),
       NULL,
       'USER',
       NULL
FROM perf_sequence
WHERE n <= 1000;

INSERT INTO post (
    post_num, profile_id, title, content, image,
    deleted_at, edited_at, write_at, version
)
SELECT n,
       MOD(n - 1, 1000) + 1,
       CONCAT('Load test post ', n),
       CONCAT('Synthetic load-test content for post ', n),
       NULL,
       NULL,
       NULL,
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL MOD(n, 1440) MINUTE),
       0
FROM perf_sequence
WHERE n <= 10000;

INSERT INTO post_stat (
    post_num, view_count, like_count, comment_count, report_count
)
SELECT n,
       MOD(n * 17, 10000),
       MOD(n * 7, 500),
       5,
       0
FROM perf_sequence
WHERE n <= 10000;

INSERT INTO comment (
    comment_num, post_num, parent_num, depth, profile_id, content,
    deleted_at, edited_at, write_at, version, child_count
)
SELECT n,
       FLOOR((n - 1) / 5) + 1,
       NULL,
       0,
       MOD(n * 31 - 1, 1000) + 1,
       CONCAT('Synthetic load-test comment ', n),
       NULL,
       NULL,
       DATE_SUB(UTC_TIMESTAMP(6), INTERVAL MOD(n, 1440) MINUTE),
       0,
       0
FROM perf_sequence
WHERE n <= 50000;

INSERT INTO post_popularity_stat (
    post_num, view_count5m, view_count30m, view_count60m, popularity_score
)
SELECT n,
       101 - n,
       (101 - n) * 3,
       (101 - n) * 6,
       (101 - n) * 1000
FROM perf_sequence
WHERE n <= 100;

INSERT INTO post_view_bucket (
    post_num, bucket_start_at, view_count
)
SELECT n,
       FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(UTC_TIMESTAMP()) / 300) * 300),
       101 - n
FROM perf_sequence
WHERE n <= 100;

ANALYZE TABLE sign_info, user_info, post, post_stat, comment,
              post_popularity_stat, post_view_bucket;

DROP TEMPORARY TABLE perf_sequence;
DROP TEMPORARY TABLE perf_digit;
