CREATE TABLE sys_user
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username      VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
    display_name  VARCHAR(50)  NOT NULL COMMENT '用户显示名称',
    role          VARCHAR(20)  NOT NULL COMMENT '角色',
    status        VARCHAR(20)  NOT NULL COMMENT '账号状态',
    created_at    DATETIME     NOT NULL COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    INDEX idx_sys_user_role_status (role, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '系统用户表';
