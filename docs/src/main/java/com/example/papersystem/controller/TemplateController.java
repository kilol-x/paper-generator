package com.example.papersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Template;
import com.example.papersystem.entity.TemplateConfig;
import com.example.papersystem.mapper.TemplateConfigMapper;
import com.example.papersystem.mapper.TemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/templates")
public class TemplateController {

    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    private TemplateConfigMapper templateConfigMapper;

    @GetMapping
    public Result<List<Template>> list(@RequestParam(required = false) Integer collegeId,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Template> wrapper = new LambdaQueryWrapper<>();
        if (collegeId != null) wrapper.eq(Template::getCollegeId, collegeId);
        if (type != null && !type.isEmpty()) wrapper.eq(Template::getType, type);
        if (status != null) wrapper.eq(Template::getStatus, status);
        wrapper.orderByDesc(Template::getUpdateTime);
        return Result.success("查询成功", templateMapper.selectList(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            return Result.error(404, "模板不存在");
        }
        TemplateConfig config = templateConfigMapper.selectOne(
                new LambdaQueryWrapper<TemplateConfig>().eq(TemplateConfig::getTemplateId, id));

        Map<String, Object> data = new HashMap<>();
        data.put("template", template);
        data.put("config", config);
        return Result.success("查询成功", data);
    }

    @PostMapping
    public Result<Long> add(@RequestBody Map<String, Object> body) {
        Template template = extractTemplate(body);
        if (template.getName() == null || template.getType() == null || template.getCollegeId() == null) {
            return Result.error(400, "模板名称、类型、所属学院不能为空");
        }
        template.setStatus(template.getStatus() == null ? 0 : template.getStatus());
        template.setVersion(1);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.insert(template);

        TemplateConfig config = extractConfig(body);
        config.setTemplateId(template.getId());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        templateConfigMapper.insert(config);

        return Result.success("新增成功", template.getId());
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Template existing = templateMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "模板不存在");
        }

        Template template = extractTemplate(body);
        template.setId(id);
        template.setVersion(existing.getVersion() + 1);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);

        TemplateConfig config = extractConfig(body);
        TemplateConfig existingConfig = templateConfigMapper.selectOne(
                new LambdaQueryWrapper<TemplateConfig>().eq(TemplateConfig::getTemplateId, id));
        if (existingConfig != null) {
            config.setId(existingConfig.getId());
            config.setTemplateId(id);
            config.setUpdateTime(LocalDateTime.now());
            templateConfigMapper.updateById(config);
        } else {
            config.setTemplateId(id);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            templateConfigMapper.insert(config);
        }
        return Result.success("修改成功", null);
    }

    @PutMapping("/{id}/status")
    public Result<String> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        Template template = templateMapper.selectById(id);
        if (template == null) {
            return Result.error(404, "模板不存在");
        }
        template.setStatus(status);
        template.setUpdateTime(LocalDateTime.now());
        templateMapper.updateById(template);
        return Result.success(status == 1 ? "已启用" : "已停用", null);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        if (templateMapper.selectById(id) == null) {
            return Result.error(404, "模板不存在");
        }
        templateMapper.deleteById(id);
        templateConfigMapper.delete(
                new LambdaQueryWrapper<TemplateConfig>().eq(TemplateConfig::getTemplateId, id));
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
