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

    // {"sections":[{key,name,required,visible,editable,order,multiLevel?,maxLevel?,note?},...]}
    // required=学生是否必填；visible=模板中是否显示该节（两者独立）
    @Lob
    @Column(name = "structure_json", columnDefinition = "LONGTEXT")
    private String structureJson;

    // 包含 page/body/heading1~3/abstract/caption/references/coverTitle/coverBody/header/footer
    // header.text 支持占位符 {collegeName}；references.numberFormat 用 [N] 表示编号位置
    @Lob
    @Column(name = "format_json", columnDefinition = "LONGTEXT")
    private String formatJson;

    // [{key,label,type(text|date),required,maxLength,order},...] 仅定义字段，样式由 formatJson.coverTitle/coverBody 控制
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
