package com.example.papersystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_template",
        indexes = {
                @Index(name = "idx_college", columnList = "college_id"),
                @Index(name = "idx_type", columnList = "type"),
                @Index(name = "idx_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "college_id", nullable = false)
    private Integer collegeId;

    @Column(nullable = false)
    private Integer status = 0;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(length = 500)
    private String description;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
