package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.College;
import com.example.papersystem.entity.Template;
import com.example.papersystem.entity.TemplateConfig;
import com.example.papersystem.repository.CollegeRepository;
import com.example.papersystem.repository.TemplateConfigRepository;
import com.example.papersystem.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 学生端只读的模板 / 学院查询接口
 * 与 /api/admin/templates 区分：此处只返回启用状态（status=1）模板；只有查询，无写操作。
 */
@RestController
public class StudentTemplateController {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateConfigRepository templateConfigRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    /** 学生端：获取所有启用状态的模板（可选按学院/类型筛选） */
    @GetMapping("/api/templates")
    public Result<List<Template>> listEnabled(@RequestParam(required = false) Integer collegeId,
                                              @RequestParam(required = false) String type) {
        // 强制只返回 status=1 已启用的模板
        List<Template> data = templateRepository.findAll(
                TemplateRepository.filter(collegeId, type, 1));
        return Result.success("查询成功", data);
    }

    /** 学生端：获取单个模板详情（含 config），用于套用模板时快照 */
    @GetMapping("/api/templates/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Optional<Template> opt = templateRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.error(404, "模板不存在");
        }
        Template tpl = opt.get();
        if (tpl.getStatus() == null || tpl.getStatus() != 1) {
            return Result.error(403, "模板已停用，无法套用");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("template", tpl);
        data.put("config", templateConfigRepository.findByTemplateId(id).orElse(null));
        return Result.success("查询成功", data);
    }

    /** 学生端：查询所有学院（用于模板选择时按学院筛选） */
    @GetMapping("/api/colleges")
    public Result<List<College>> listColleges() {
        return Result.success("查询成功", collegeRepository.findAllByOrderByCreateTimeDesc());
    }
}
