package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.entity.PaperVersion;
import com.example.papersystem.entity.ReferenceEntry;
import com.example.papersystem.entity.User;
import com.example.papersystem.repository.PaperVersionRepository;
import com.example.papersystem.repository.ReferenceEntryRepository;
import com.example.papersystem.repository.UserRepository;
import com.example.papersystem.service.PaperVersionSnapshotService;
import com.example.papersystem.service.PaperService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    @Autowired
    private PaperService paperService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private PaperVersionRepository paperVersionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferenceEntryRepository referenceEntryRepository;

    @Autowired
    private PaperVersionSnapshotService paperVersionSnapshotService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 辅助方法 ====================

    /** 从 JWT 中提取当前用户 ID */
    private Long getCurrentUserId() {
        Claims claims = (Claims) request.getAttribute("claims");
        if (claims == null)
            return null;
        return ((Number) claims.get("userId")).longValue();
    }

    /** 将 Paper 实体转为前端需要的响应（content JSON → sections 数组） */
    private Map<String, Object> buildPaperResponse(Paper paper) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", paper.getId());
        map.put("title", paper.getTitle());
        map.put("studentId", paper.getStudentId());
        map.put("studentName", paper.getStudentName());
        map.put("studentIdNumber", paper.getStudentIdNumber());
        map.put("status", paper.getStatus());
        map.put("teacherId", paper.getTeacherId());
        map.put("locked", paper.getLocked());
        map.put("currentVersion", paper.getCurrentVersion());
        map.put("score", paper.getScore());
        map.put("grade", paper.getGrade());
        map.put("teacherSummary", paper.getTeacherSummary());
        map.put("templateId", paper.getTemplateId());
        map.put("templateSnapshot", paper.getTemplateSnapshot());
        map.put("createdAt", paper.getCreatedAt());
        map.put("updatedAt", paper.getUpdatedAt());
        map.put("references", paperVersionSnapshotService.normalizeReferences(
                referenceEntryRepository.findByPaperIdOrderByCitationNoAscCreatedAtAsc(paper.getId())));

        // 将 content JSON 还原为 sections 数组
        String content = paper.getContent();
        if (content != null && !content.isBlank()) {
            try {
                List<Map<String, Object>> sections = objectMapper.readValue(
                        content, new TypeReference<List<Map<String, Object>>>() {
                        });
                map.put("sections", sections);
            } catch (JsonProcessingException e) {
                // content 可能不是 JSON（例如直接粘贴的文本），当作原始内容返回
                map.put("sections", List.of());
                map.put("rawContent", content);
            }
        } else {
            map.put("sections", List.of());
        }
        return map;
    }

    // ==================== 接口 ====================

    /** 获取论文列表（分页 + 可选筛选） */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或 Token 已过期");
        }

        Page<Paper> paperPage = paperService.findAll(userId, page, size, status, keyword);

        // 列表只返回元数据，不加载 content/sections
        List<Map<String, Object>> items = paperPage.getContent().stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("title", p.getTitle());
            item.put("status", p.getStatus());
            item.put("locked", p.getLocked());
            item.put("currentVersion", p.getCurrentVersion());
            item.put("score", p.getScore());
            item.put("grade", p.getGrade());
            item.put("teacherSummary", p.getTeacherSummary());
            item.put("createdAt", p.getCreatedAt());
            item.put("updatedAt", p.getUpdatedAt());
            return item;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("content", items);
        result.put("totalElements", paperPage.getTotalElements());
        result.put("totalPages", paperPage.getTotalPages());
        result.put("number", paperPage.getNumber());
        result.put("size", paperPage.getSize());

        return Result.success("查询成功", result);
    }

    /** 获取单篇论文 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Paper paper = paperService.findById(id);
        if (paper == null) {
            return Result.error(404, "论文不存在");
        }
        Long userId = getCurrentUserId();
        if (userId == null)
            return Result.error(401, "未登录或 Token 已过期");
        if (!userId.equals(paper.getStudentId()))
            return Result.error(403, "无权访问他人论文");
        return Result.success("查询成功", buildPaperResponse(paper));
    }

    /** 获取草稿历史（自动保存/手动保存） */
    @GetMapping("/{id}/drafts")
    public Result<List<PaperVersion>> drafts(@PathVariable Long id) {
        Paper paper = paperService.findById(id);
        if (paper == null) {
            return Result.error(404, "论文不存在");
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或 Token 已过期");
        }
        if (!userId.equals(paper.getStudentId())) {
            return Result.error(403, "无权访问他人论文");
        }

        List<PaperVersion> list = paperVersionRepository.findByPaperIdOrderByVersionNoDescCreatedAtDesc(id)
                .stream()
                .filter(v -> "AUTO_SAVE".equals(v.getAction()) || "MANUAL_SAVE".equals(v.getAction())
                        || "CREATE".equals(v.getAction()))
                .toList();
        return Result.success("查询成功", list);
    }

    @GetMapping("/{id}/drafts/{versionId}")
    public Result<Map<String, Object>> draftDetail(@PathVariable Long id, @PathVariable Long versionId) {
        Paper paper = paperService.findById(id);
        if (paper == null) {
            return Result.error(404, "论文不存在");
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或 Token 已过期");
        }
        if (!userId.equals(paper.getStudentId())) {
            return Result.error(403, "无权访问他人论文");
        }

        PaperVersion version = paperVersionRepository.findById(versionId).orElse(null);
        if (version == null || !id.equals(version.getPaperId())) {
            return Result.error(404, "历史版本不存在");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", version.getId());
        detail.put("paperId", version.getPaperId());
        detail.put("versionNo", version.getVersionNo());
        detail.put("action", version.getAction());
        detail.put("description", version.getDescription());
        detail.put("createdAt", version.getCreatedAt());
        detail.put("snapshot", paperVersionSnapshotService.parseSnapshot(version.getContentSnapshot()));
        return Result.success("查询成功", detail);
    }

    /** 兼容旧前端：手动创建版本记录 */
    @PostMapping("/{id}/versions")
    public Result<PaperVersion> createVersion(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Paper paper = paperService.findById(id);
        if (paper == null) {
            return Result.error(404, "论文不存在");
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或 Token 已过期");
        }
        if (!userId.equals(paper.getStudentId())) {
            return Result.error(403, "无权为他人论文创建版本");
        }

        String action = normalizeSaveMode(body.get("action"));
        String description = body.get("description") == null ? "保存草稿" : body.get("description").toString();
        String snapshot = body.get("contentSnapshot") == null
                ? paperVersionSnapshotService.buildSnapshot(paper,
                        referenceEntryRepository.findByPaperIdOrderByCitationNoAscCreatedAtAsc(id))
                : body.get("contentSnapshot").toString();
        PaperVersion version = recordDraftVersion(paper, action, description, userId, snapshot);
        return Result.success("记录成功", version);
    }

    /** 保存新论文 */
    @PostMapping
    public Result<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        String title = body.get("title") != null ? body.get("title").toString().trim() : null;
        if (title == null || title.isBlank()) {
            return Result.error(400, "论文标题不能为空");
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或 Token 已过期");
        }
        User student = userRepository.findById(userId).orElse(null);
        if (student == null || student.getTeacherId() == null) {
            return Result.error(409, "当前学生尚未绑定教师，无法创建论文");
        }

        Paper paper = new Paper();
        paper.setTitle(title);
        paper.setStudentId(userId);
        paper.setStudentName(student.getNickname());
        paper.setStudentIdNumber(student.getUsername());
        paper.setTeacherId(student.getTeacherId());
        paper.setStatus("DRAFT");
        paper.setLocked(false);

        // 前端传来的 sections 数组序列化为 JSON 存入 content
        Object sections = body.get("sections");
        if (sections != null) {
            try {
                paper.setContent(objectMapper.writeValueAsString(sections));
            } catch (JsonProcessingException e) {
                return Result.error(500, "论文数据序列化失败");
            }
        }

        // 模板关联
        Object templateId = body.get("templateId");
        if (templateId != null)
            paper.setTemplateId(((Number) templateId).longValue());
        Object snapshot = body.get("templateSnapshot");
        if (snapshot != null)
            paper.setTemplateSnapshot(snapshot.toString());

        Paper saved = paperService.save(paper);
        List<ReferenceEntry> references = syncReferences(saved.getId(), body.get("references"));
        String saveMode = normalizeSaveMode(body.get("saveMode"));
        recordDraftVersion(saved, saveMode, "CREATE".equals(saveMode) ? "创建论文草稿" : "保存论文草稿", userId,
                paperVersionSnapshotService.buildSnapshot(saved, references));
        return Result.success("保存成功", buildPaperResponse(saved));
    }

    /** 更新论文 */
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Paper existing = paperService.findById(id);
        if (existing == null) {
            return Result.error(404, "论文不存在");
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或 Token 已过期");
        }

        // 仅允许论文作者修改
        if (!userId.equals(existing.getStudentId())) {
            return Result.error(403, "无权修改他人论文");
        }
        if (Boolean.TRUE.equals(existing.getLocked())) {
            return Result.error(409, "论文已锁定，当前状态不可编辑");
        }
        if (!("DRAFT".equals(existing.getStatus()) || "RETURNED".equals(existing.getStatus()))) {
            return Result.error(409, "当前论文状态不可编辑");
        }

        Paper updated = new Paper();
        if (body.get("title") != null) {
            updated.setTitle(body.get("title").toString().trim());
        }
        // sections → content JSON
        Object sections = body.get("sections");
        if (sections != null) {
            try {
                updated.setContent(objectMapper.writeValueAsString(sections));
            } catch (JsonProcessingException e) {
                return Result.error(500, "论文数据序列化失败");
            }
        }

        // 模板关联更新（切换模板时前端会带新的 templateId + snapshot）
        Object templateId = body.get("templateId");
        if (templateId != null)
            updated.setTemplateId(((Number) templateId).longValue());
        Object snapshot = body.get("templateSnapshot");
        if (snapshot != null)
            updated.setTemplateSnapshot(snapshot.toString());

        Paper saved = paperService.update(id, updated);
        List<ReferenceEntry> references = syncReferences(saved.getId(), body.get("references"));
        String saveMode = normalizeSaveMode(body.get("saveMode"));
        String description = "AUTO_SAVE".equals(saveMode) ? "自动保存草稿" : "手动保存草稿";
        recordDraftVersion(saved, saveMode, description, userId,
                paperVersionSnapshotService.buildSnapshot(saved, references));
        return Result.success("更新成功", buildPaperResponse(saved));
    }

    /** 删除论文 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Paper paper = paperService.findById(id);
        if (paper == null) {
            return Result.error(404, "论文不存在");
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录或 Token 已过期");
        }

        if (!userId.equals(paper.getStudentId())) {
            return Result.error(403, "无权删除他人论文");
        }
        if (Boolean.TRUE.equals(paper.getLocked())) {
            return Result.error(409, "论文已锁定，不能删除");
        }

        paperService.delete(id);
        return Result.success("删除成功", null);
    }

    private String normalizeSaveMode(Object source) {
        if (source == null) {
            return "MANUAL_SAVE";
        }
        String text = source.toString().trim().toUpperCase();
        if ("AUTO_SAVE".equals(text) || "MANUAL_SAVE".equals(text) || "CREATE".equals(text)) {
            return text;
        }
        if ("SAVE".equals(text)) {
            return "MANUAL_SAVE";
        }
        return "MANUAL_SAVE";
    }

    private List<ReferenceEntry> syncReferences(Long paperId, Object rawReferences) {
        if (rawReferences == null) {
            return referenceEntryRepository.findByPaperIdOrderByCitationNoAscCreatedAtAsc(paperId);
        }

        List<ReferenceEntry> existing = referenceEntryRepository.findByPaperIdOrderByCitationNoAscCreatedAtAsc(paperId);
        if (!existing.isEmpty()) {
            referenceEntryRepository.deleteAll(existing);
        }

        if (!(rawReferences instanceof List<?> items)) {
            return List.of();
        }

        List<ReferenceEntry> entries = new ArrayList<>();
        int citationNo = 1;
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            ReferenceEntry entry = toReferenceEntry(paperId, citationNo, map);
            if (entry == null) {
                continue;
            }
            entries.add(entry);
            citationNo++;
        }

        if (entries.isEmpty()) {
            return List.of();
        }
        return referenceEntryRepository.saveAll(entries);
    }

    private ReferenceEntry toReferenceEntry(Long paperId, int citationNo, Map<?, ?> source) {
        String authors = str(source.get("authors"));
        String title = str(source.get("title"));
        String journal = str(source.get("journal"));
        String year = str(source.get("year"));
        String pages = str(source.get("pages"));
        if (authors.isBlank() && title.isBlank() && journal.isBlank() && year.isBlank() && pages.isBlank()) {
            return null;
        }

        ReferenceEntry entry = new ReferenceEntry();
        entry.setPaperId(paperId);
        entry.setAuthors(authors);
        entry.setTitle(title);
        entry.setJournal(journal);
        entry.setYear(year);
        entry.setPages(pages);
        entry.setCitationNo(citationNo);
        entry.setFormattedText(formatReference(entry));
        return entry;
    }

    private String str(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String formatReference(ReferenceEntry entry) {
        List<String> segments = new ArrayList<>();
        if (!str(entry.getAuthors()).isBlank()) {
            segments.add(entry.getAuthors());
        }
        if (!str(entry.getTitle()).isBlank()) {
            segments.add(entry.getTitle());
        }

        StringBuilder publication = new StringBuilder();
        if (!str(entry.getJournal()).isBlank()) {
            publication.append(entry.getJournal());
        }
        if (!str(entry.getYear()).isBlank()) {
            if (publication.length() > 0) {
                publication.append(", ");
            }
            publication.append(entry.getYear());
        }
        if (!str(entry.getPages()).isBlank()) {
            if (publication.length() > 0) {
                publication.append(": ");
            }
            publication.append(entry.getPages());
        }
        if (publication.length() > 0) {
            segments.add(publication.toString());
        }

        String text = segments.isEmpty() ? "未命名文献" : String.join(". ", segments);
        if (!text.endsWith(".")) {
            text = text + ".";
        }
        return text;
    }

    private PaperVersion recordDraftVersion(Paper paper, String action, String description, Long operatorId,
            String snapshot) {
        if (paper == null) {
            return null;
        }
        int nextVersion = Optional.ofNullable(paper.getCurrentVersion()).orElse(0) + 1;
        paper.setCurrentVersion(nextVersion);
        if (paper.getStatus() == null || paper.getStatus().isBlank()) {
            paper.setStatus("DRAFT");
        }
        Paper latest = paperService.save(paper);

        PaperVersion version = new PaperVersion();
        version.setPaperId(latest.getId());
        version.setVersionNo(nextVersion);
        version.setAction(action);
        version.setDescription(description);
        version.setOperatorId(operatorId);
        version.setContentSnapshot(snapshot);
        return paperVersionRepository.save(version);
    }
}
