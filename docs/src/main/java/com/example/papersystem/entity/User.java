package com.example.papersystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tb_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String role; // ADMIN, STUDENT, TEACHER
    private Integer deptId;
    private String email;
    private Integer status; // 1-正常, 0-禁用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}