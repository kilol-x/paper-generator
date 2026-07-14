package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.College;
import com.example.papersystem.repository.CollegeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/colleges")
public class CollegeController {

    @Autowired
    private CollegeRepository collegeRepository;

    @GetMapping
    public Result<List<College>> list(@RequestParam(required = false) String keyword) {
        List<College> data = (keyword == null || keyword.isEmpty())
                ? collegeRepository.findAllByOrderByCreateTimeDesc()
                : collegeRepository.findByNameContainingOrderByCreateTimeDesc(keyword);
        return Result.success("查询成功", data);
    }

    @GetMapping("/{id}")
    public Result<College> getById(@PathVariable Integer id) {
        return collegeRepository.findById(id)
                .map(c -> Result.success("查询成功", c))
                .orElse(Result.error(404, "学院不存在"));
    }

    @PostMapping
    public Result<String> add(@RequestBody College college) {
        if (college.getName() == null || college.getName().isEmpty()) {
            return Result.error(400, "学院名称不能为空");
        }
        if (collegeRepository.existsByName(college.getName())) {
            return Result.error(400, "学院名称已存在");
        }
        college.setId(null);
        collegeRepository.save(college);
        return Result.success("新增成功", null);
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody College college) {
        Optional<College> opt = collegeRepository.findById(id);
        if (opt.isEmpty()) {
            return Result.error(404, "学院不存在");
        }
        College existing = opt.get();
        if (college.getName() != null) existing.setName(college.getName());
        if (college.getDescription() != null) existing.setDescription(college.getDescription());
        collegeRepository.save(existing);
        return Result.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        if (!collegeRepository.existsById(id)) {
            return Result.error(404, "学院不存在");
        }
        collegeRepository.deleteById(id);
        return Result.success("删除成功", null);
    }
}
