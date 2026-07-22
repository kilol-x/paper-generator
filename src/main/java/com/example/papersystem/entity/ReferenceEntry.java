package com.example.papersystem.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "reference_entry")
@EntityListeners(AuditingEntityListener.class)
public class ReferenceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paper_id", nullable = false)
    private Long paperId;

    @Column(nullable = false, length = 500)
    private String authors;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(name = "journal_name", length = 1000)
    private String journal;

    @Column(name = "publish_year", length = 20)
    private String year;

    @Column(length = 100)
    private String pages;

    @Column(name = "citation_no", nullable = false)
    private Integer citationNo;

    @Column(name = "formatted_text", length = 4000)
    private String formattedText;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}