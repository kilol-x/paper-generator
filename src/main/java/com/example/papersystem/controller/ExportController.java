package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.service.PaperExportService;
import com.example.papersystem.service.PaperPreviewService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ExportController {

    private final PaperExportService paperExportService;
    private final PaperPreviewService paperPreviewService;
    private final PaperRepository paperRepository;

    /**
     * 校验当前用户是否有权访问指定论文。
     * 规则：学生只能访问自己的论文，教师只能访问名下学生的论文，管理员可访问所有。
     */
    private Paper requireAccess(Long paperId, HttpServletRequest request) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("论文不存在"));

        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) throw new SecurityException("登录信息无效");

        Long userId = ((Number) claims.get("userId")).longValue();
        String role = claims.get("role", String.class);

        boolean allowed;
        if ("ADMIN".equals(role)) {
            allowed = true;
        } else if ("TEACHER".equals(role)) {
            allowed = userId.equals(paper.getTeacherId());
        } else {
            allowed = userId.equals(paper.getStudentId());
        }

        if (!allowed) throw new SecurityException("无权访问该论文");
        return paper;
    }

    /**
     * 获取论文预览数据（用于前端展示）
     * GET /api/papers/{paperId}/preview
     */
    @GetMapping("/api/papers/{paperId}/preview")
    public Result<?> getPreview(@PathVariable Long paperId, HttpServletRequest request) {
        requireAccess(paperId, request);
        return Result.success("获取预览数据成功",
                paperPreviewService.buildPreview(paperId));
    }

    /**
     * 导出 PDF
     * GET /api/papers/{paperId}/export/pdf
     */
    @GetMapping("/api/papers/{paperId}/export/pdf")
    public ResponseEntity<Resource> exportPdf(@PathVariable Long paperId, HttpServletRequest request) {
        requireAccess(paperId, request);
        return paperExportService.exportPdf(paperId);
    }

    /**
     * 导出 DOCX
     * GET /api/papers/{paperId}/export/docx
     */
    @GetMapping("/api/papers/{paperId}/export/docx")
    public ResponseEntity<Resource> exportDocx(@PathVariable Long paperId, HttpServletRequest request) {
        requireAccess(paperId, request);
        return paperExportService.exportDocx(paperId);
    }
}
