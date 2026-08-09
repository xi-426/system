CREATE TABLE activity
(
    id                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    organizer_id            BIGINT       NOT NULL COMMENT '组织者ID',
    title                   VARCHAR(100) NOT NULL COMMENT '活动标题',
    category                VARCHAR(30)  NOT NULL COMMENT '活动分类',
    location                VARCHAR(200) NOT NULL COMMENT '活动地点',
    description             VARCHAR(2000)         COMMENT '活动介绍',
    capacity                INT          NOT NULL COMMENT '人数上限',
    registered_count        INT          NOT NULL DEFAULT 0 COMMENT '已预约人数',
    registration_start_time DATETIME     NOT NULL COMMENT '报名开始时间',
    registration_end_time   DATETIME     NOT NULL COMMENT '报名结束时间',
    activity_start_time     DATETIME     NOT NULL COMMENT '活动开始时间',
    activity_end_time       DATETIME     NOT NULL COMMENT '活动结束时间',
    status                  VARCHAR(30)  NOT NULL COMMENT '活动状态',
    version                 INT          NOT NULL DEFAULT 0 COMMENT '版本号',
    created_at              DATETIME     NOT NULL COMMENT '创建时间',
    updated_at              DATETIME     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_activity_status_start (status, activity_start_time),
    INDEX idx_activity_organizer (organizer_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '活动表';
