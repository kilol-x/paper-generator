package com.example.papersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_template_config")
public class TemplateConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String structureJson;
    private String formatJson;
    private String coverFields;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
