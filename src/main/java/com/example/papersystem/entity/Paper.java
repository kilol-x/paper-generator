package com.example.papersystem.entity;

<<<<<<< HEAD
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "paper")
@EntityListeners(AuditingEntityListener.class)
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
=======
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
>>>>>>> 2e14bb3414e7a360b48c444f56eb6df4daf1f733
    private LocalDateTime updatedAt;
}
