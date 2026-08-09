CREATE TABLE activity_waitlist
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '候补ID',
    activity_id  BIGINT      NOT NULL COMMENT '活动ID',
    user_id      BIGINT      NOT NULL COMMENT '学生用户ID',
    status       VARCHAR(20) NOT NULL COMMENT '候补状态',
    joined_at    DATETIME    NOT NULL COMMENT '进入候补时间',
    promoted_at  DATETIME             COMMENT '递补成功时间',
    cancelled_at DATETIME             COMMENT '退出候补时间',
    created_at   DATETIME    NOT NULL COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_waitlist_activity_user (activity_id, user_id),
    INDEX idx_waitlist_activity_order (activity_id, status, joined_at, id),
    INDEX idx_waitlist_user_status (user_id, status),
    CONSTRAINT fk_waitlist_activity
        FOREIGN KEY (activity_id) REFERENCES activity (id),
    CONSTRAINT fk_waitlist_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '活动候补表';
