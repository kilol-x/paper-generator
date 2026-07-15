package com.example.papersystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_template_config",
        uniqueConstraints = @UniqueConstraint(columnNames = "template_id"))
@EntityListeners(AuditingEntityListener.class)
public class TemplateConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Lob
    @Column(name = "structure_json", columnDefinition = "LONGTEXT")
    private String structureJson;

    @Lob
    @Column(name = "format_json", columnDefinition = "LONGTEXT")
    private String formatJson;

    @Lob
    @Column(name = "cover_fields", columnDefinition = "LONGTEXT")
    private String coverFields;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
