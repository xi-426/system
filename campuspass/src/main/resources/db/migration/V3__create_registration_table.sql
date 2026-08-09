CREATE TABLE activity_registration
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    activity_id BIGINT      NOT NULL COMMENT '活动ID',
    user_id     BIGINT      NOT NULL COMMENT '学生用户ID',
    status      VARCHAR(20) NOT NULL COMMENT '预约状态',
    reserved_at DATETIME    NOT NULL COMMENT '预约时间',
    cancelled_at DATETIME            COMMENT '取消时间',
    created_at  DATETIME    NOT NULL COMMENT '创建时间',
    updated_at  DATETIME    NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_registration_activity_user (activity_id, user_id),
    INDEX idx_registration_user_status (user_id, status),
    INDEX idx_registration_activity_status (activity_id, status),
    CONSTRAINT fk_registration_activity
        FOREIGN KEY (activity_id) REFERENCES activity (id),
    CONSTRAINT fk_registration_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '活动预约表';
