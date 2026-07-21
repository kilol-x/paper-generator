package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.service.PaperExportService;
import com.example.papersystem.service.PaperPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ExportController {

    private final PaperExportService paperExportService;
    private final PaperPreviewService paperPreviewService;

    /**
     * 获取论文预览数据（用于前端展示）
     * GET /api/papers/{paperId}/preview
     */
    @GetMapping("/api/papers/{paperId}/preview")
    public Result<?> getPreview(@PathVariable Long paperId) {
        return Result.success("获取预览数据成功",
                paperPreviewService.buildPreview(paperId));
    }

    /**
     * 导出 PDF
     * GET /api/papers/{paperId}/export/pdf
     */
    @GetMapping("/api/papers/{paperId}/export/pdf")
    public ResponseEntity<Resource> exportPdf(@PathVariable Long paperId) {
        return paperExportService.exportPdf(paperId);
    }

    /**
     * 导出 DOCX
     * GET /api/papers/{paperId}/export/docx
     */
    @GetMapping("/api/papers/{paperId}/export/docx")
    public ResponseEntity<Resource> exportDocx(@PathVariable Long paperId) {
        return paperExportService.exportDocx(paperId);
    }
}
