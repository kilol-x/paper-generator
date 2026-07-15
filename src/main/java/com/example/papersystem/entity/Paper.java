package com.example.papersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 论文实体类
 */
@Data
@TableName("paper") // 对应数据库表名 paper
public class Paper {

    @TableId(type = IdType.AUTO) // 主键自增
    private Long id;

    private String title;       // 论文标题

    private Long studentId;     // 所属学生ID

    private String content;     // 论文内容(JSON格式字符串)

    private String status;      // 状态：DRAFT(草稿) / SUBMITTED(已提交)

    private LocalDateTime createdAt; // 创建时间

    private LocalDateTime updatedAt; // 更新时间
}