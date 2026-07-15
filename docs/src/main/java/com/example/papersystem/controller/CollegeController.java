package com.example.papersystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.papersystem.common.Result;
import com.example.papersystem.entity.College;
import com.example.papersystem.mapper.CollegeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/colleges")
public class CollegeController {

    @Autowired
    private CollegeMapper collegeMapper;

    @GetMapping
    public Result<List<College>> list(@RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<College> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(College::getName, keyword);
        }
        wrapper.orderByDesc(College::getCreateTime);
        return Result.success("查询成功", collegeMapper.selectList(wrapper));
    }

    @GetMapping("/{id}")
    public Result<College> getById(@PathVariable Integer id) {
        College college = collegeMapper.selectById(id);
        if (college == null) {
            return Result.error(404, "学院不存在");
        }
        return Result.success("查询成功", college);
    }

    @PostMapping
    public Result<String> add(@RequestBody College college) {
        if (college.getName() == null || college.getName().isEmpty()) {
            return Result.error(400, "学院名称不能为空");
        }
        Long count = collegeMapper.selectCount(
                new LambdaQueryWrapper<College>().eq(College::getName, college.getName()));
        if (count > 0) {
            return Result.error(400, "学院名称已存在");
        }
        college.setCreateTime(LocalDateTime.now());
        college.setUpdateTime(LocalDateTime.now());
        collegeMapper.insert(college);
        return Result.success("新增成功", null);
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody College college) {
        College existing = collegeMapper.selectById(id);
        if (existing == null) {
            return Result.error(404, "学院不存在");
        }
        college.setId(id);
        college.setUpdateTime(LocalDateTime.now());
        collegeMapper.updateById(college);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        if (collegeMapper.selectById(id) == null) {
            return Result.error(404, "学院不存在");
        }
        collegeMapper.deleteById(id);
        return Result.success("删除成功", null);
    }
}
