package com.example.papersystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "paper_reference")
public class PaperReference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paper_id", nullable = false)
    private Long paperId;

    @Column(name = "author", length = 200)
    private String author;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "journal", length = 200)
    private String journal;

    @Column(name = "publisher", length = 200)
    private String publisher;

    @Column(name = "university", length = 200)
    private String university;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "year", length = 20)
    private String year;

    @Column(name = "volume", length = 50)
    private String volume;

    @Column(name = "issue", length = 50)
    private String issue;

    @Column(name = "page", length = 50)
    private String page;

    @Column(name = "type", length = 50)
    private String type;  // 期刊/专著/学位论文/电子文献

    @Column(name = "ref_index")
    private Integer index;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}