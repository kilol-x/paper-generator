package com.example.papersystem.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaperPreviewDTO {
    private Long paperId;
    private String title;
    private String fullHtml;          // 完整的论文 HTML（用于预览）
    private List<ChapterDTO> chapters;
    private List<ReferenceDTO> references;
    private List<String> images;      // 图片 Base64 或 URL
    private Long templateId;
    private String templateName;
    private Long studentId;
    private String studentName;

    @Data
    public static class ChapterDTO {
        private String id;
        private String title;
        private String content;        // HTML 格式
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
        private String type;           // 期刊/专著/学位论文
    }
}