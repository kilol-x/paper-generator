package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Template;
import com.example.papersystem.entity.TemplateConfig;
import com.example.papersystem.repository.TemplateConfigRepository;
import com.example.papersystem.repository.TemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/templates")
public class TemplateController {

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private TemplateConfigRepository templateConfigRepository;

    @GetMapping
    public Result<List<Template>> list(@RequestParam(required = false) Integer collegeId,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) Integer status) {
        List<Template> data = templateRepository.findAll(
                TemplateRepository.filter(collegeId, type, status));
        return Result.success("查询成功", data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Optional<Template> opt = templateRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.error(404, "模板不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("template", opt.get());
        data.put("config", templateConfigRepository.findByTemplateId(id).orElse(null));
        return Result.success("查询成功", data);
    }

    @PostMapping
    @Transactional
    public Result<Long> add(@RequestBody Map<String, Object> body) {
        Template template = extractTemplate(body);
        if (template.getName() == null || template.getType() == null || template.getCollegeId() == null) {
            return Result.error(400, "模板名称、类型、所属学院不能为空");
        }
        if (template.getStatus() == null) template.setStatus(0);
        template.setVersion(1);
        templateRepository.save(template);

        TemplateConfig config = extractConfig(body);
        config.setTemplateId(template.getId());
        templateConfigRepository.save(config);

        return Result.success("新增成功", template.getId());
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<String> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<Template> opt = templateRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.error(404, "模板不存在");
        }
        Template existing = opt.get();
        Template patch = extractTemplate(body);
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getType() != null) existing.setType(patch.getType());
        if (patch.getCollegeId() != null) existing.setCollegeId(patch.getCollegeId());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getStatus() != null) existing.setStatus(patch.getStatus());
        existing.setVersion(existing.getVersion() + 1);
        templateRepository.save(existing);

        TemplateConfig patchConfig = extractConfig(body);
        TemplateConfig config = templateConfigRepository.findByTemplateId(id)
                .orElseGet(() -> {
                    TemplateConfig c = new TemplateConfig();
                    c.setTemplateId(id);
                    return c;
                });
        if (patchConfig.getStructureJson() != null) config.setStructureJson(patchConfig.getStructureJson());
        if (patchConfig.getFormatJson() != null) config.setFormatJson(patchConfig.getFormatJson());
        if (patchConfig.getCoverFields() != null) config.setCoverFields(patchConfig.getCoverFields());
        templateConfigRepository.save(config);

        return Result.success("修改成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<String> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        Optional<Template> opt = templateRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.error(404, "模板不存在");
        }
        Template template = opt.get();
        template.setStatus(status);
        templateRepository.save(template);
        return Result.success(status == 1 ? "已启用" : "已停用", null);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<String> delete(@PathVariable Long id) {
        if (!templateRepository.existsById(id)) {
            return Result.error(404, "模板不存在");
        }
        templateRepository.deleteById(id);
        templateConfigRepository.deleteByTemplateId(id);
        return Result.success("删除成功", null);
    }

    private Template extractTemplate(Map<String, Object> body) {
        Template t = new Template();
        t.setName((String) body.get("name"));
        t.setType((String) body.get("type"));
        Object collegeId = body.get("collegeId");
        if (collegeId != null) t.setCollegeId(((Number) collegeId).intValue());
        Object status = body.get("status");
        if (status != null) t.setStatus(((Number) status).intValue());
        t.setDescription((String) body.get("description"));
        return t;
    }

    private TemplateConfig extractConfig(Map<String, Object> body) {
        TemplateConfig c = new TemplateConfig();
        c.setStructureJson((String) body.get("structureJson"));
        c.setFormatJson((String) body.get("formatJson"));
        c.setCoverFields((String) body.get("coverFields"));
        return c;
    }
}
