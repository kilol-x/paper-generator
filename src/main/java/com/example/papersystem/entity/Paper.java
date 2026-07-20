package com.example.papersystem.entity;

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

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(nullable = false)
    private Boolean locked = false;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion = 1;

    private Integer score;

    @Column(length = 4)
    private String grade;

    @Column(name = "teacher_summary", length = 2000)
    private String teacherSummary;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
