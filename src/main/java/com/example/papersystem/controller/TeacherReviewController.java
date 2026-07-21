package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.entity.PaperVersion;
import com.example.papersystem.entity.ReviewComment;
import com.example.papersystem.entity.User;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.repository.PaperVersionRepository;
import com.example.papersystem.repository.ReviewCommentRepository;
import com.example.papersystem.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class TeacherReviewController {
    private final PaperRepository papers;
    private final ReviewCommentRepository comments;
    private final PaperVersionRepository versions;
    private final UserRepository users;

    public TeacherReviewController(PaperRepository papers, ReviewCommentRepository comments,
                                   PaperVersionRepository versions, UserRepository users) {
        this.papers = papers; this.comments = comments; this.versions = versions; this.users = users;
    }

    @PostMapping("/{id}/submit")
    @Transactional
    public Result<Paper> submit(@PathVariable Long id, HttpServletRequest request) {
        Paper paper = papers.findById(id).orElseThrow(() -> new IllegalArgumentException("论文不存在"));
        Long studentId = currentUserId(request);
        if (!studentId.equals(paper.getStudentId())) return Result.error(403, "无权提交他人论文");
        if (Boolean.TRUE.equals(paper.getLocked())) return Result.error(409, "论文已锁定，不能重复提交");
        if (!("DRAFT".equals(paper.getStatus()) || "RETURNED".equals(paper.getStatus()))) return Result.error(409, "当前状态不可提交");
        User student = users.findById(studentId).orElseThrow();
        if (student.getTeacherId() == null) return Result.error(409, "当前学生尚未绑定教师，无法提交论文");
        paper.setTeacherId(student.getTeacherId());
        paper.setStudentName(student.getNickname());
        paper.setStudentIdNumber(student.getUsername());
        paper.setCurrentVersion((paper.getCurrentVersion() == null ? 0 : paper.getCurrentVersion()) + 1);
        paper.setStatus("SUBMITTED");
        paper.setLocked(true);
        paper.setScore(null); paper.setGrade(null); paper.setTeacherSummary(null);
        Paper saved = papers.save(paper);
        PaperVersion version = new PaperVersion();
        version.setPaperId(saved.getId()); version.setVersionNo(saved.getCurrentVersion());
        version.setContentSnapshot(saved.getContent()); version.setAction("SUBMIT");
        version.setDescription("学生提交论文 V" + saved.getCurrentVersion()); version.setOperatorId(studentId);
        versions.save(version);
        return Result.success("提交成功，论文已同步至绑定教师", saved);
    }

    @PostMapping("/{id}/comments")
    @Transactional
    public Result<ReviewComment> comment(@PathVariable Long id, @RequestBody ReviewComment input, HttpServletRequest request) {
        Paper paper = reviewable(id, request);
        if (input.getSectionKey() == null || input.getSectionKey().isBlank() || input.getContent() == null || input.getContent().isBlank()) return Result.error(400, "批注位置和内容不能为空");
        ReviewComment comment = new ReviewComment();
        comment.setPaperId(id); comment.setPaperVersion(paper.getCurrentVersion());
        comment.setSectionKey(input.getSectionKey()); comment.setAnchorText(input.getAnchorText());
        comment.setContent(input.getContent().trim()); comment.setTeacherId(currentUserId(request));
        paper.setStatus("REVIEWING"); papers.save(paper);
        return Result.success("批注已保存", comments.save(comment));
    }

    @PostMapping("/{id}/return")
    @Transactional
    public Result<Paper> returnForRevision(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Paper paper = reviewable(id, request);
        String summary = String.valueOf(body.getOrDefault("summary", "")).trim();
        if (summary.isBlank()) return Result.error(400, "驳回时必须填写修改意见");
        applyScore(paper, body.get("score"));
        paper.setTeacherSummary(summary); paper.setStatus("RETURNED"); paper.setLocked(false);
        return Result.success("论文已驳回，学生端已解除编辑锁", papers.save(paper));
    }

    @PostMapping({"/{id}/approve", "/{id}/grade"})
    @Transactional
    public Result<Paper> approve(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Paper paper = reviewable(id, request);
        String summary = String.valueOf(body.getOrDefault("summary", "")).trim();
        if (body.get("score") == null || summary.isBlank()) return Result.error(400, "审核通过前请填写分数和评语");
        applyScore(paper, body.get("score"));
        paper.setTeacherSummary(summary); paper.setStatus("APPROVED"); paper.setLocked(true);
        return Result.success("审核通过，论文已锁定", papers.save(paper));
    }

    @GetMapping("/{id}/comments")
    public Result<List<ReviewComment>> comments(@PathVariable Long id, HttpServletRequest request) {
        accessible(id, request); return Result.success("查询成功", comments.findByPaperIdOrderByCreatedAtAsc(id));
    }

    @GetMapping("/{id}/versions")
    public Result<List<PaperVersion>> versions(@PathVariable Long id, HttpServletRequest request) {
        accessible(id, request); return Result.success("查询成功", versions.findByPaperIdOrderByVersionNoDescCreatedAtDesc(id));
    }

    private Paper reviewable(Long id, HttpServletRequest request) {
        Paper paper = papers.findById(id).orElseThrow(() -> new IllegalArgumentException("论文不存在"));
        if (!currentUserId(request).equals(paper.getTeacherId())) throw new SecurityException("无权批阅其他教师的学生论文");
        if (!Boolean.TRUE.equals(paper.getLocked()) || !("SUBMITTED".equals(paper.getStatus()) || "REVIEWING".equals(paper.getStatus()))) throw new IllegalStateException("当前状态不可批阅");
        return paper;
    }

    private void accessible(Long id, HttpServletRequest request) {
        Paper paper = papers.findById(id).orElseThrow(() -> new IllegalArgumentException("论文不存在"));
        Claims claims = claims(request); Long userId = ((Number) claims.get("userId")).longValue(); String role = claims.get("role", String.class);
        boolean allowed = "TEACHER".equals(role) ? userId.equals(paper.getTeacherId()) : "STUDENT".equals(role) && userId.equals(paper.getStudentId());
        if (!allowed) throw new SecurityException("无权查看该论文批阅记录");
    }

    private void applyScore(Paper paper, Object value) {
        if (value == null || String.valueOf(value).isBlank()) return;
        int score = Integer.parseInt(String.valueOf(value));
        if (score < 0 || score > 100) throw new IllegalArgumentException("分数应为0至100");
        paper.setScore(score); paper.setGrade(score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : score >= 60 ? "D" : "F");
    }

    private Claims claims(HttpServletRequest request) {
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null) throw new SecurityException("登录信息无效");
        return claims;
    }
    private Long currentUserId(HttpServletRequest request) { return ((Number) claims(request).get("userId")).longValue(); }

    @ExceptionHandler(IllegalArgumentException.class) public Result<Object> badRequest(IllegalArgumentException e) { return Result.error(400, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) public Result<Object> conflict(IllegalStateException e) { return Result.error(409, e.getMessage()); }
    @ExceptionHandler(SecurityException.class) public Result<Object> forbidden(SecurityException e) { return Result.error(403, e.getMessage()); }
}
