package com.example.papersystem.service;

import com.example.papersystem.entity.Paper;
import com.example.papersystem.entity.PaperChapter;
import com.example.papersystem.entity.PaperReference;
import com.example.papersystem.repository.PaperChapterRepository;
import com.example.papersystem.repository.PaperReferenceRepository;
import com.example.papersystem.repository.PaperRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperExportService {

    private final PaperRepository paperRepository;
    private final PaperChapterRepository chapterRepository;
    private final PaperReferenceRepository referenceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从 Paper.content JSON 或 PaperChapter 中提取章节列表
     */
    private List<ChapterInfo> extractChapters(Paper paper) {
        List<ChapterInfo> result = new ArrayList<>();

        // 优先从 PaperChapter 表读取
        List<PaperChapter> dbChapters = chapterRepository.findByPaperIdOrderBySortOrderAsc(paper.getId());
        if (dbChapters != null && !dbChapters.isEmpty()) {
            for (PaperChapter pc : dbChapters) {
                result.add(new ChapterInfo(pc.getTitle(), pc.getContent(), pc.getLevel()));
            }
            return result;
        }

        // 回退：从 Paper.content JSON 解析
        if (paper.getContent() != null && !paper.getContent().isBlank()) {
            try {
                List<Map<String, Object>> sections = objectMapper.readValue(
                        paper.getContent(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> sec : sections) {
                    String type = (String) sec.getOrDefault("type", "chapter");
                    // 只处理普通章节
                    if ("chapter".equals(type) || type == null) {
                        String title = (String) sec.getOrDefault("title", "");
                        String content = (String) sec.getOrDefault("content", "");
                        int level = sec.get("level") instanceof Number ? ((Number) sec.get("level")).intValue() : 1;
                        result.add(new ChapterInfo(title, content, level));
                    }
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    /**
     * 从 Paper.content JSON 或 PaperReference 中提取参考文献
     */
    private List<PaperReference> extractReferences(Paper paper) {
        List<PaperReference> dbRefs = referenceRepository.findByPaperIdOrderByIndexAsc(paper.getId());
        if (dbRefs != null && !dbRefs.isEmpty()) {
            return dbRefs;
        }

        // 从 content JSON 中解析 references 特殊节
        if (paper.getContent() != null && !paper.getContent().isBlank()) {
            try {
                List<Map<String, Object>> sections = objectMapper.readValue(
                        paper.getContent(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> sec : sections) {
                    if ("references".equals(sec.get("type"))) {
                        String content = (String) sec.getOrDefault("content", "[]");
                        List<Map<String, Object>> refs = objectMapper.readValue(
                                content, new TypeReference<List<Map<String, Object>>>() {});
                        List<PaperReference> result = new ArrayList<>();
                        for (Map<String, Object> r : refs) {
                            PaperReference pr = new PaperReference();
                            pr.setAuthor((String) r.get("authors"));
                            pr.setTitle((String) r.get("title"));
                            pr.setJournal((String) r.get("journal"));
                            pr.setYear((String) r.get("year"));
                            pr.setVolume((String) r.get("volume"));
                            pr.setIssue((String) r.get("issue"));
                            pr.setPage((String) r.get("pages"));
                            pr.setType((String) r.get("type"));
                            result.add(pr);
                        }
                        return result;
                    }
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>();
    }

    /**
     * 导出 PDF
     */
    public ResponseEntity<Resource> exportPdf(Long paperId) {
        try {
            Paper paper = paperRepository.findById(paperId)
                    .orElseThrow(() -> new RuntimeException("论文不存在"));

            List<ChapterInfo> chapters = extractChapters(paper);
            List<PaperReference> references = extractReferences(paper);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 72, 72);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font chFont = getChineseFont(12, Font.NORMAL);
            Font titleFont = getChineseFont(22, Font.BOLD);
            Font h1Font = getChineseFont(18, Font.BOLD);
            Font h2Font = getChineseFont(16, Font.BOLD);
            Font h3Font = getChineseFont(14, Font.BOLD);
            Font coverFont = getChineseFont(14, Font.NORMAL);

            // 封面
            addEmptyLine(document, 6);
            Paragraph titleP = new Paragraph(paper.getTitle() != null ? paper.getTitle() : "论文题目", titleFont);
            titleP.setAlignment(Element.ALIGN_CENTER);
            document.add(titleP);
            addEmptyLine(document, 4);

            String[] lines = {
                "姓    名：" + (paper.getStudentName() != null ? paper.getStudentName() : ""),
                "学    号：" + (paper.getStudentIdNumber() != null ? paper.getStudentIdNumber() : ""),
                "日    期：" + java.time.LocalDate.now().toString()
            };
            for (String line : lines) {
                if (!line.endsWith("：") && !line.endsWith(":")) {
                    Paragraph p = new Paragraph(line, coverFont);
                    p.setIndentationLeft(180);
                    document.add(p);
                    addEmptyLine(document, 1);
                }
            }
            document.newPage();

            // 章节
            for (ChapterInfo ch : chapters) {
                int level = ch.level != null ? ch.level : 0;
                Font f = level == 0 ? h1Font : level == 1 ? h2Font : h3Font;
                boolean centered = level == 0;

                Paragraph chP = new Paragraph(ch.title != null ? ch.title : "", f);
                chP.setAlignment(centered ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
                chP.setSpacingBefore(12);
                chP.setSpacingAfter(8);
                document.add(chP);

                if (ch.content != null && !ch.content.isBlank()) {
                    String text = ch.content.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").replace("&amp;", "&");
                    Paragraph cp = new Paragraph(text, chFont);
                    cp.setFirstLineIndent(24);
                    cp.setSpacingAfter(4);
                    document.add(cp);
                }
            }

            // 参考文献
            if (!references.isEmpty()) {
                document.newPage();
                Paragraph rTitle = new Paragraph("参考文献", h1Font);
                rTitle.setAlignment(Element.ALIGN_CENTER);
                rTitle.setSpacingAfter(16);
                document.add(rTitle);

                int idx = 1;
                for (PaperReference ref : references) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[").append(idx++).append("] ");
                    if (ref.getAuthor() != null || ref.getAuthor() != null) {
                        sb.append(ref.getAuthor() != null ? ref.getAuthor() : ref.getAuthor()).append(". ");
                    }
                    if (ref.getTitle() != null) sb.append("《").append(ref.getTitle()).append("》");
                    if (ref.getType() != null) {
                        sb.append(switch (ref.getType()) {
                            case "期刊" -> "[J]";
                            case "专著" -> "[M]";
                            case "学位论文" -> "[D]";
                            default -> "[EB/OL]";
                        });
                    }
                    if (ref.getJournal() != null) sb.append(". ").append(ref.getJournal());
                    if (ref.getYear() != null) sb.append(", ").append(ref.getYear());
                    if (ref.getVolume() != null) sb.append(", ").append(ref.getVolume());
                    if (ref.getIssue() != null) sb.append("(").append(ref.getIssue()).append(")");
                    if (ref.getPage() != null || ref.getPage() != null) {
                        sb.append(": ").append(ref.getPage() != null ? ref.getPage() : ref.getPage());
                    }
                    sb.append(".");

                    Paragraph rp = new Paragraph(sb.toString(), getChineseFont(10, Font.NORMAL));
                    rp.setSpacingAfter(3);
                    document.add(rp);
                }
            }

            document.close();
            ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
            String filename = (paper.getTitle() != null ? paper.getTitle() : "论文") + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(baos.size())
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("PDF导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 导出 DOCX（简易版，主要供后端使用）
     */
    public ResponseEntity<Resource> exportDocx(Long paperId) {
        try {
            Paper paper = paperRepository.findById(paperId)
                    .orElseThrow(() -> new RuntimeException("论文不存在"));
            List<ChapterInfo> chapters = extractChapters(paper);

            org.apache.poi.xwpf.usermodel.XWPFDocument doc =
                    new org.apache.poi.xwpf.usermodel.XWPFDocument();

            // 标题
            org.apache.poi.xwpf.usermodel.XWPFParagraph titleP = doc.createParagraph();
            titleP.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            org.apache.poi.xwpf.usermodel.XWPFRun titleR = titleP.createRun();
            titleR.setText(paper.getTitle() != null ? paper.getTitle() : "论文题目");
            titleR.setBold(true);
            titleR.setFontSize(22);
            titleR.setFontFamily("SimHei");

            // 章节
            for (ChapterInfo ch : chapters) {
                org.apache.poi.xwpf.usermodel.XWPFParagraph chP = doc.createParagraph();
                int level = ch.level != null ? ch.level : 0;
                chP.setAlignment(level == 0
                        ? org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER
                        : org.apache.poi.xwpf.usermodel.ParagraphAlignment.LEFT);
                chP.setSpacingBefore(300);
                chP.setSpacingAfter(200);

                org.apache.poi.xwpf.usermodel.XWPFRun chR = chP.createRun();
                chR.setText(ch.title != null ? ch.title : "");
                chR.setBold(true);
                chR.setFontSize(level == 0 ? 18 : level == 1 ? 16 : 14);
                chR.setFontFamily("SimHei");

                if (ch.content != null && !ch.content.isBlank()) {
                    String cleanText = ch.content.replaceAll("<[^>]*>", "").replace("&nbsp;", " ");
                    org.apache.poi.xwpf.usermodel.XWPFParagraph ctP = doc.createParagraph();
                    ctP.setFirstLineIndent(480);
                    ctP.setSpacingAfter(100);
                    org.apache.poi.xwpf.usermodel.XWPFRun ctR = ctP.createRun();
                    ctR.setText(cleanText);
                    ctR.setFontSize(12);
                    ctR.setFontFamily("SimSun");
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.write(baos);
            doc.close();

            ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
            String filename = (paper.getTitle() != null ? paper.getTitle() : "论文") + ".docx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .contentLength(baos.size())
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("DOCX导出失败: " + e.getMessage(), e);
        }
    }

    // ── 内部类 ──
    private static class ChapterInfo {
        String title;
        String content;
        Integer level;
        ChapterInfo(String title, String content, Integer level) {
            this.title = title; this.content = content; this.level = level;
        }
    }

    // ── 字体 ──
    private Font getChineseFont(float size, int style) {
        String[][] fonts = {
            {"C:/Windows/Fonts/simsun.ttc", "0"},
            {"C:/Windows/Fonts/simhei.ttf", null},
            {"C:/Windows/Fonts/msyh.ttc", "0"},
            {"C:/Windows/Fonts/msyhbd.ttc", "0"},
            {"C:/Windows/Fonts/msyhl.ttc", "0"}
        };
        for (String[] f : fonts) {
            try {
                if (Files.exists(Paths.get(f[0]))) {
                    String path = f[1] != null ? f[0] + "," + f[1] : f[0];
                    BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
                    return new Font(bf, size, style);
                }
            } catch (Exception ignored) {
                continue;
            }
        }
        // 所有字体都不可用 - 返回默认字体
        return new Font(Font.FontFamily.HELVETICA, size, style);
    }

    private void addEmptyLine(Document doc, int count) throws DocumentException {
        for (int i = 0; i < count; i++) doc.add(new Paragraph(" "));
    }
}

