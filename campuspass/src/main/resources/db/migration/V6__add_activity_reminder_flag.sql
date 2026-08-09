ALTER TABLE activity
    ADD COLUMN reminder_sent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已发送开场提醒'
    AFTER version;
