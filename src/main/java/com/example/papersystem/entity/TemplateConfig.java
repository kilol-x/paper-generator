package com.example.papersystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tb_template_config")
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

    // 添加这个字段，这样 @Data 注解会自动为你生成 getCoverFields() 方法
    @Lob
    @Column(name = "cover_fields", columnDefinition = "LONGTEXT")
    private String coverFields;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}