package com.example.papersystem.service.impl;

import com.example.papersystem.entity.Paper;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperRepository paperRepository;

    @Override
    public Paper createPaper(Paper paper, Long studentId) {
        paper.setStudentId(studentId);
        paper.setStatus("DRAFT");
        return paperRepository.save(paper);
    }

    @Override
    public Paper getPaperById(Long id) {
        return paperRepository.findById(id).orElse(null);
    }

    @Override
    public List<Paper> getPapersByStudentId(Long studentId) {
        return paperRepository.findByStudentIdOrderByUpdatedAtDesc(studentId);
    }

    @Override
    public void updatePaperContent(Long id, String content) {
        paperRepository.findById(id).ifPresent(paper -> {
            paper.setContent(content);
            paperRepository.save(paper);
        });
    }

    @Override
    public void deletePaper(Long id) {
        paperRepository.deleteById(id);
    }
}
