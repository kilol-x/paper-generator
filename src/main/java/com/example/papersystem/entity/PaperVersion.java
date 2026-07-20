package com.example.papersystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "paper_version", uniqueConstraints = @UniqueConstraint(columnNames = {"paper_id", "version_no"}))
@EntityListeners(AuditingEntityListener.class)
public class PaperVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paper_id", nullable = false)
    private Long paperId;
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;
    @Lob @Column(columnDefinition = "LONGTEXT")
    private String contentSnapshot;
    @Column(nullable = false, length = 30)
    private String action;
    @Column(length = 1000)
    private String description;
    @Column(name = "operator_id")
    private Long operatorId;
    @CreatedDate @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
