package com.example.papersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_paper")
public class Paper {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long studentId;
    private String content;
    private String status; // DRAFT, SUBMITTED, REVIEWED, APPROVED, REJECTED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
