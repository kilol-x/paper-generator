package com.example.papersystem.service;

import com.example.papersystem.dto.PaperPreviewDTO;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.entity.PaperChapter;
import com.example.papersystem.entity.PaperReference;
import com.example.papersystem.entity.Template;
import com.example.papersystem.repository.PaperChapterRepository;
import com.example.papersystem.repository.PaperReferenceRepository;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperPreviewService {

    private final PaperRepository paperRepository;
    private final PaperChapterRepository chapterRepository;
    private final PaperReferenceRepository referenceRepository;
    private final TemplateRepository templateRepository;

    /**
     * 构建论文预览数据
     */
    public PaperPreviewDTO buildPreview(Long paperId) {
        // 1. 获取论文基本信息
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        // 2. 获取模板信息（优先从数据库，降级使用快照）

        Template template = null;
        if (paper.getTemplateId() != null) {
            template = templateRepository.findById(paper.getTemplateId()).orElse(null);
        }
        // 如果模板已被删除或从未绑定，构造一个占位模板对象供渲染使用
        String templateName = "未绑定模板";

        if (template != null) {
            templateName = template.getName();
        } else if (paper.getTemplateSnapshot() != null) {
            // 尝试从快照中解析模板名称
            try {
                var snapshotMap = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(paper.getTemplateSnapshot(), java.util.Map.class);
                Object name = snapshotMap.get("templateName");
                if (name != null) templateName = name.toString();
            } catch (Exception ignored) {
            }
        }

        // 3. 获取章节列表
        List<PaperChapter> chapters = chapterRepository.findByPaperIdOrderBySortOrderAsc(paperId);

        // 4. 获取参考文献
        List<PaperReference> references = referenceRepository.findByPaperIdOrderByIndexAsc(paperId);

        // 5. 组装 DTO
        PaperPreviewDTO dto = new PaperPreviewDTO();
        dto.setPaperId(paper.getId());
        dto.setTitle(paper.getTitle());
        dto.setTemplateId(template != null ? template.getId() : paper.getTemplateId());
        dto.setTemplateName(templateName);
        dto.setStudentId(paper.getStudentId());
        dto.setStudentName(paper.getStudentName() != null ? paper.getStudentName() : "未命名");

        // 6. 转换章节
        List<PaperPreviewDTO.ChapterDTO> chapterDTOs = chapters.stream()
                .map(this::convertToChapterDTO)
                .collect(Collectors.toList());
        dto.setChapters(chapterDTOs);

        // 7. 转换参考文献
        List<PaperPreviewDTO.ReferenceDTO> referenceDTOs = new ArrayList<>();
        int index = 1;
        for (PaperReference ref : references) {
            PaperPreviewDTO.ReferenceDTO refDTO = convertToReferenceDTO(ref, index);
            referenceDTOs.add(refDTO);
            index++;
        }
        dto.setReferences(referenceDTOs);

        // 8. 生成完整 HTML
        String fullHtml = buildFullHtml(paper, templateName, chapterDTOs, referenceDTOs);
        dto.setFullHtml(fullHtml);

        // 9. 图片列表（暂空，后续扩展）
        dto.setImages(new ArrayList<>());

        return dto;
    }

    /**
     * ✅ 将 PaperChapter 转换为 ChapterDTO
     */
    private PaperPreviewDTO.ChapterDTO convertToChapterDTO(PaperChapter chapter) {
        PaperPreviewDTO.ChapterDTO dto = new PaperPreviewDTO.ChapterDTO();
        dto.setId(String.valueOf(chapter.getId()));
        dto.setTitle(chapter.getTitle());
        dto.setContent(chapter.getContent() != null ? chapter.getContent() : "");
        dto.setLevel(chapter.getLevel() != null ? chapter.getLevel() : 0);
        dto.setSortOrder(chapter.getSortOrder() != null ? chapter.getSortOrder() : 0);
        dto.setParentId(chapter.getParentId() != null ? String.valueOf(chapter.getParentId()) : null);
        return dto;
    }

    /**
     * ✅ 将 PaperReference 转换为 ReferenceDTO
     */
    private PaperPreviewDTO.ReferenceDTO convertToReferenceDTO(PaperReference ref, int index) {
        PaperPreviewDTO.ReferenceDTO dto = new PaperPreviewDTO.ReferenceDTO();
        dto.setIndex(index);
        dto.setAuthor(ref.getAuthor() != null ? ref.getAuthor() : "");
        dto.setTitle(ref.getTitle() != null ? ref.getTitle() : "");
        dto.setJournal(ref.getJournal() != null ? ref.getJournal() : "");
        dto.setYear(ref.getYear() != null ? ref.getYear() : "");
        dto.setVolume(ref.getVolume() != null ? ref.getVolume() : "");
        dto.setIssue(ref.getIssue() != null ? ref.getIssue() : "");
        dto.setPage(ref.getPage() != null ? ref.getPage() : "");
        dto.setType(ref.getType() != null ? ref.getType() : "");
        return dto;
    }

    /**
     * 构建完整论文 HTML
     */
    private String buildFullHtml(Paper paper, String templateName,
                                 List<PaperPreviewDTO.ChapterDTO> chapters,
                                 List<PaperPreviewDTO.ReferenceDTO> references) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>").append(paper.getTitle()).append("</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: SimSun, 'Times New Roman', serif; font-size: 12pt; ");
        html.append("margin: 2.54cm 3.17cm; line-height: 1.5; }\n");
        html.append("    h1 { font-family: SimHei; font-size: 18pt; text-align: center; margin: 20px 0; }\n");
        html.append("    h2 { font-family: SimHei; font-size: 16pt; margin: 16px 0; }\n");
        html.append("    h3 { font-family: SimHei; font-size: 14pt; margin: 12px 0; }\n");
        html.append("    p { text-indent: 2em; margin: 0.5em 0; }\n");
        html.append("    .cover-title { font-size: 22pt; text-align: center; font-weight: bold; margin-top: 60px; }\n");
        html.append("    .cover-info { text-align: center; font-size: 16pt; margin-top: 40px; }\n");
        html.append("    table { border-collapse: collapse; margin: 12px auto; width: 100%; }\n");
        html.append("    th, td { border: 1px solid #333; padding: 6px 10px; text-align: center; }\n");
        html.append("    img { max-width: 100%; display: block; margin: 10px auto; }\n");
        html.append("    .ref-list { list-style: none; padding-left: 0; }\n");
        html.append("    .ref-list li { margin: 4px 0; text-indent: -2em; padding-left: 2em; }\n");
        html.append("    .page-break { page-break-after: always; border: none; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        // ===== 封面 =====
        html.append("<div class=\"cover\">\n");
        html.append("  <h1 class=\"cover-title\">").append(paper.getTitle()).append("</h1>\n");
        html.append("  <div class=\"cover-info\">\n");
        html.append("    <p>学生：").append(paper.getStudentName() != null ? paper.getStudentName() : "").append("</p>\n");
        html.append("    <p>模板：").append(templateName).append("</p>\n");
        html.append("    <p>日期：").append(LocalDate.now()).append("</p>\n");
        html.append("  </div>\n");
        html.append("</div>\n");

        html.append("<hr class=\"page-break\">\n");

        // ===== 原创声明 =====
        html.append("<h1 style=\"text-align:center;\">原创性声明</h1>\n");
        html.append("<p>本人郑重声明：所呈交的论文是本人在指导教师指导下独立完成的研究成果。");
        html.append("除文中已经注明引用的内容外，本论文不包含任何其他个人或集体已经发表或撰写的成果作品。</p>\n");

        // ===== 各章节 =====
        for (PaperPreviewDTO.ChapterDTO chapter : chapters) {
            html.append(buildChapterHtml(chapter));
        }

        // ===== 参考文献 =====
        if (!references.isEmpty()) {
            html.append("<h1 style=\"text-align:center;\">参考文献</h1>\n");
            html.append("<ol class=\"ref-list\">\n");
            for (PaperPreviewDTO.ReferenceDTO ref : references) {
                html.append("  <li>")
                        .append(ref.getAuthor()).append(". ")
                        .append(ref.getTitle());

                if ("期刊".equals(ref.getType())) {
                    html.append("[J]. ").append(ref.getJournal())
                            .append(", ").append(ref.getYear())
                            .append(", ").append(ref.getVolume())
                            .append("(").append(ref.getIssue()).append("): ")
                            .append(ref.getPage()).append(".");
                } else if ("专著".equals(ref.getType())) {
                    html.append("[M]. ").append(ref.getJournal())
                            .append(", ").append(ref.getYear()).append(".");
                } else if ("学位论文".equals(ref.getType())) {
                    html.append("[D]. ").append(ref.getJournal())
                            .append(", ").append(ref.getYear()).append(".");
                } else {
                    html.append("[EB/OL]. ").append(ref.getJournal());
                }

                html.append("</li>\n");
            }
            html.append("</ol>\n");
        }

        // ===== 致谢 =====
        html.append("<h1 style=\"text-align:center;\">致谢</h1>\n");
        html.append("<p>在本次论文撰写过程中，感谢指导教师的悉心指导和同学们的热心帮助。</p>\n");

        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 构建单个章节 HTML
     */
    private String buildChapterHtml(PaperPreviewDTO.ChapterDTO chapter) {
        StringBuilder html = new StringBuilder();

        String title = chapter.getTitle();
        int level = chapter.getLevel() != null ? chapter.getLevel() : 0;

        if (level == 0) {
            html.append("<h1 style=\"text-align:center;\">").append(title).append("</h1>\n");
        } else if (level == 1) {
            html.append("<h2>").append(title).append("</h2>\n");
        } else if (level == 2) {
            html.append("<h3>").append(title).append("</h3>\n");
        } else {
            html.append("<h4>").append(title).append("</h4>\n");
        }

        if (chapter.getContent() != null && !chapter.getContent().isEmpty()) {
            html.append(chapter.getContent()).append("\n");
        }

        return html.toString();
    }
}