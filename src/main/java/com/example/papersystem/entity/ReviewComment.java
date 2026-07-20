package com.example.papersystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review_comment")
@EntityListeners(AuditingEntityListener.class)
public class ReviewComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paper_id", nullable = false)
    private Long paperId;
    @Column(name = "paper_version", nullable = false)
    private Integer paperVersion;
    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;
    @Column(name = "anchor_text", length = 500)
    private String anchorText;
    @Column(nullable = false, length = 2000)
    private String content;
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;
    @CreatedDate @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
