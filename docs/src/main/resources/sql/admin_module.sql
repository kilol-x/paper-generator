-- 学院表
CREATE TABLE IF NOT EXISTS tb_college (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL COMMENT '学院名称',
    description VARCHAR(500)          COMMENT '学院简介',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学院表';

-- 模板主表
CREATE TABLE IF NOT EXISTS tb_template (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL COMMENT '模板名称',
    type        VARCHAR(50)  NOT NULL COMMENT '模板类型: THESIS毕业论文 COURSE课程论文 PROJECT项目论文',
    college_id  INT          NOT NULL COMMENT '所属学院ID',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 1启用 0停用',
    version     INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    description VARCHAR(500)          COMMENT '模板描述',
    create_time DATETIME     NOT NULL,
    update_time DATETIME     NOT NULL,
    KEY idx_college (college_id),
    KEY idx_type (type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论文模板表';

-- 模板配置表（章节结构 + 格式配置）
CREATE TABLE IF NOT EXISTS tb_template_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id     BIGINT NOT NULL COMMENT '关联模板ID',
    structure_json  LONGTEXT COMMENT '章节结构JSON',
    format_json     LONGTEXT COMMENT '格式配置JSON',
    cover_fields    LONGTEXT COMMENT '封面字段定义JSON',
    create_time     DATETIME NOT NULL,
    update_time     DATETIME NOT NULL,
    UNIQUE KEY uk_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板配置表';
