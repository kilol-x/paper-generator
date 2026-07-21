package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.entity.User;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherWorkspaceController {
    private final UserRepository users;
    private final PaperRepository papers;

    public TeacherWorkspaceController(UserRepository users, PaperRepository papers) {
        this.users = users;
        this.papers = papers;
    }

    @PostMapping("/students")
    @Transactional
    public Result<Map<String, Object>> createStudent(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String name = text(body.get("name"));
        String studentNo = text(body.get("studentNo"));
        if (name.isBlank() || studentNo.isBlank()) return Result.error(400, "学生姓名和学号不能为空");
        if (users.existsByUsername(studentNo)) return Result.error(409, "该学号已存在，不能重复创建账号");
        User student = new User();
        student.setUsername(studentNo);
        student.setNickname(name);
        student.setPassword(BCrypt.hashpw("123456", BCrypt.gensalt()));
        student.setRole("STUDENT");
        student.setTeacherId(currentUserId(request));
        student.setStatus(1);
        return Result.success("学生账号创建成功，初始密码为123456", studentView(users.save(student)));
    }

    @GetMapping("/students")
    public Result<List<Map<String, Object>>> students(HttpServletRequest request) {
        List<Map<String, Object>> data = users.findByTeacherIdAndRoleOrderByCreateTimeDesc(currentUserId(request), "STUDENT")
                .stream().map(student -> {
                    Map<String, Object> item = studentView(student);
                    item.put("submittedPaperCount", papers.countByStudentIdAndStatusNot(student.getId(), "DRAFT"));
                    return item;
                }).toList();
        return Result.success("查询成功", data);
    }

    @GetMapping("/papers")
    public Result<List<Map<String, Object>>> papers(HttpServletRequest request) {
        List<Map<String, Object>> data = papers.findByTeacherIdAndStatusNotOrderByUpdatedAtDesc(currentUserId(request), "DRAFT")
                .stream().map(this::paperView).toList();
        return Result.success("查询成功", data);
    }

    @GetMapping("/papers/{id}")
    public Result<Map<String, Object>> paper(@PathVariable Long id, HttpServletRequest request) {
        Paper paper = papers.findById(id).orElse(null);
        if (paper == null) return Result.error(404, "论文不存在");
        if (!currentUserId(request).equals(paper.getTeacherId())) return Result.error(403, "无权查看其他教师的学生论文");
        return Result.success("查询成功", paperView(paper));
    }

    private Map<String, Object> studentView(User student) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", student.getId());
        item.put("name", student.getNickname());
        item.put("studentNo", student.getUsername());
        item.put("status", student.getStatus());
        item.put("createTime", student.getCreateTime());
        return item;
    }

    private Map<String, Object> paperView(Paper paper) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("paper", paper);
        User student = users.findById(paper.getStudentId()).orElse(null);
        item.put("studentName", student == null ? paper.getStudentName() : student.getNickname());
        item.put("studentNo", student == null ? paper.getStudentIdNumber() : student.getUsername());
        return item;
    }

    private Long currentUserId(HttpServletRequest request) {
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) throw new SecurityException("登录信息无效");
        return ((Number) claims.get("userId")).longValue();
    }

    private String text(String value) { return value == null ? "" : value.trim(); }
}
