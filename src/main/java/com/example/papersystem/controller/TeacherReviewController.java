package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.entity.PaperVersion;
import com.example.papersystem.entity.ReviewComment;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.repository.PaperVersionRepository;
import com.example.papersystem.repository.ReviewCommentRepository;
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
    public TeacherReviewController(PaperRepository papers, ReviewCommentRepository comments, PaperVersionRepository versions) {
        this.papers = papers; this.comments = comments; this.versions = versions;
    }

    @PostMapping("/{id}/submit") @Transactional
    public Result<Paper> submit(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        Paper p = requirePaper(id);
        if (Boolean.TRUE.equals(p.getLocked())) return Result.error(409, "论文已锁定，不能重复提交");
        if ("RETURNED".equals(p.getStatus())) p.setCurrentVersion(p.getCurrentVersion() + 1);
        p.setTeacherId(longValue(body.get("teacherId")));
        p.setStatus("SUBMITTED"); p.setLocked(true);
        papers.save(p); record(p, "SUBMIT", "学生提交论文，系统锁定当前版本", p.getStudentId());
        return Result.success("提交成功，论文已锁定", p);
    }

    @PostMapping("/{id}/comments") @Transactional
    public Result<ReviewComment> comment(@PathVariable Long id, @RequestBody ReviewComment c) {
        Paper p = requireReviewable(id);
        if (c.getSectionKey() == null || c.getContent() == null || c.getContent().isBlank()) return Result.error(400, "批注位置和内容不能为空");
        c.setId(null); c.setPaperId(id); c.setPaperVersion(p.getCurrentVersion());
        p.setStatus("REVIEWING"); papers.save(p);
        ReviewComment saved = comments.save(c); record(p, "COMMENT", "教师在文档位置添加批注：" + c.getSectionKey(), c.getTeacherId());
        return Result.success("批注已保存", saved);
    }

    @PostMapping("/{id}/return") @Transactional
    public Result<Paper> returnForRevision(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        Paper p = requireReviewable(id);
        p.setTeacherSummary(String.valueOf(body.getOrDefault("summary", "")));
        p.setStatus("RETURNED"); p.setLocked(false); papers.save(p);
        record(p, "RETURN", "教师退回修改，学生端解除锁定", p.getTeacherId());
        return Result.success("已退回，学生可继续修改", p);
    }

    @PostMapping("/{id}/grade") @Transactional
    public Result<Paper> grade(@PathVariable Long id, @RequestBody Map<String,Object> body) {
        Paper p = requireReviewable(id);
        int score = Integer.parseInt(String.valueOf(body.getOrDefault("score", 0)));
        String summary = String.valueOf(body.getOrDefault("summary", ""));
        if (score < 0 || score > 100 || summary.isBlank()) return Result.error(400, "分数应为0至100且总评不能为空");
        p.setScore(score); p.setGrade(score >= 90 ? "A" : score >= 80 ? "B" : score >= 70 ? "C" : score >= 60 ? "D" : "F");
        p.setTeacherSummary(summary); p.setStatus("GRADED"); p.setLocked(true); papers.save(p);
        record(p, "GRADE", "教师完成评分：" + score + "分", p.getTeacherId());
        return Result.success("评分已归档", p);
    }

    @GetMapping("/{id}/comments")
    public Result<List<ReviewComment>> comments(@PathVariable Long id) { requirePaper(id); return Result.success("查询成功", comments.findByPaperIdOrderByCreatedAtAsc(id)); }
    @GetMapping("/{id}/versions")
    public Result<List<PaperVersion>> versions(@PathVariable Long id) { requirePaper(id); return Result.success("查询成功", versions.findByPaperIdOrderByVersionNoDescCreatedAtDesc(id)); }

    private Paper requirePaper(Long id) { return papers.findById(id).orElseThrow(() -> new IllegalArgumentException("论文不存在")); }
    private Paper requireReviewable(Long id) { Paper p=requirePaper(id); if (!Boolean.TRUE.equals(p.getLocked()) || !("SUBMITTED".equals(p.getStatus()) || "REVIEWING".equals(p.getStatus()))) throw new IllegalStateException("当前状态不可批阅"); return p; }
    private void record(Paper p,String action,String description,Long operatorId){ PaperVersion v=new PaperVersion();v.setPaperId(p.getId());v.setVersionNo(p.getCurrentVersion());v.setContentSnapshot(p.getContent());v.setAction(action);v.setDescription(description);v.setOperatorId(operatorId);versions.save(v); }
    private Long longValue(Object v){ return v==null?null:Long.valueOf(String.valueOf(v)); }

    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    public Result<Object> handle(RuntimeException e){ return Result.error(e instanceof IllegalArgumentException?404:409,e.getMessage()); }
}
