CREATE TABLE notification
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    event_id   VARCHAR(64)  NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    content    VARCHAR(500) NOT NULL,
    read_flag  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_event (event_id),
    INDEX idx_notification_user_created (user_id, created_at),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE message_consume_record
(
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    event_id    VARCHAR(64) NOT NULL,
    consumed_at DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consume_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_checkin
(
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    activity_id BIGINT   NOT NULL,
    user_id     BIGINT   NOT NULL,
    checked_at  DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_checkin_activity_user (activity_id, user_id),
    CONSTRAINT fk_checkin_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
    CONSTRAINT fk_checkin_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE activity_registration
    ADD COLUMN checked_in_at DATETIME NULL COMMENT '签到时间' AFTER cancelled_at;
