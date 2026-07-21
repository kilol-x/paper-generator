package com.example.papersystem.controller;

import com.example.papersystem.dto.PaperPreviewDTO;
import com.example.papersystem.service.PaperPreviewService;
import com.example.papersystem.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/papers")
@RequiredArgsConstructor
public class PaperPreviewController {

    private final PaperPreviewService paperPreviewService;

    /**
     * 获取论文预览数据
     * GET /api/student/papers/{paperId}/preview
     */
    @GetMapping("/{paperId}/preview")
    public Result<PaperPreviewDTO> getPreview(@PathVariable Long paperId) {
        PaperPreviewDTO previewData = paperPreviewService.buildPreview(paperId);
        return Result.success("获取预览数据成功", previewData);
    }
}