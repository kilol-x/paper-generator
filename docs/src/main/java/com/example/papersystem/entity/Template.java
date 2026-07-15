package com.example.papersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_template")
public class Template {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private Integer collegeId;
    private Integer status;
    private Integer version;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
