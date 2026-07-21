package com.example.papersystem.service.impl;

import com.example.papersystem.entity.Paper;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperRepository paperRepository;

    @Override
    public Paper save(Paper paper) {
        if (paper.getStatus() == null || paper.getStatus().isBlank()) {
            paper.setStatus("DRAFT");
        }
        return paperRepository.save(paper);
    }

    @Override
    public Paper findById(Long id) {
        return paperRepository.findById(id).orElse(null);
    }

    @Override
    public Page<Paper> findAll(Long studentId, int page, int size, String status, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        if ((status == null || status.isBlank()) && (keyword == null || keyword.isBlank())) {
            return paperRepository.findByStudentIdOrderByUpdatedAtDesc(studentId, pageable);
        }
        return paperRepository.findPapers(studentId, status, keyword, pageable);
    }

    @Override
    public Paper update(Long id, Paper updated) {
        Paper existing = paperRepository.findById(id).orElse(null);
        if (existing == null) return null;

        if (updated.getTitle() != null && !updated.getTitle().isBlank()) {
            existing.setTitle(updated.getTitle().trim());
        }
        if (updated.getContent() != null) {
            existing.setContent(updated.getContent());
        }
        if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
            existing.setStatus(updated.getStatus());
        }
        if (updated.getTemplateId() != null) {
            existing.setTemplateId(updated.getTemplateId());
        }
        if (updated.getTemplateSnapshot() != null) {
            existing.setTemplateSnapshot(updated.getTemplateSnapshot());
        }
        // updatedAt is auto-managed by @LastModifiedDate, but ensure it refreshes
        existing.setUpdatedAt(LocalDateTime.now());
        return paperRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        paperRepository.deleteById(id);
    }
}
