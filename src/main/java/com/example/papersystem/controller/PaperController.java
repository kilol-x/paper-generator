package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @PostMapping
    public Result<Paper> save(@RequestBody Paper paper) {
        if (paper.getTitle() == null || paper.getTitle().isBlank()) {
            return Result.error(400, "论文标题不能为空");
        }
        Paper saved = paperService.save(paper);
        return Result.success("保存成功", saved);
    }

    @GetMapping("/{id}")
    public Result<Paper> getById(@PathVariable Long id) {
        Paper paper = paperService.findById(id);
        if (paper == null) {
            return Result.error(404, "论文不存在");
        }
        return Result.success("查询成功", paper);
    }
}
