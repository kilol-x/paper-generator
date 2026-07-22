package com.example.papersystem.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaperPreviewDTO {
    private Long paperId;
    private String title;
    private String fullHtml;
    private List<ChapterDTO> chapters;
    private List<ReferenceDTO> references;
    private List<String> images;
    private Long templateId;
    private String templateName;
    private Long studentId;
    private String studentName;

    @Data
    public static class ChapterDTO {
        private String id;
        private String title;
        private String content;
        private Integer level;
        private Integer sortOrder;
        private String parentId;
    }

    @Data
    public static class ReferenceDTO {
        private Integer index;
        private String author;
        private String title;
        private String journal;
        private String year;
        private String volume;
        private String issue;
        private String page;
        private String type;
    }
}