package com.example.papersystem.service;

import com.example.papersystem.entity.Paper;

import java.util.List;

public interface PaperService {
    Paper createPaper(Paper paper, Long studentId);
    Paper getPaperById(Long id);
    List<Paper> getPapersByStudentId(Long studentId);
    void updatePaperContent(Long id, String content);
    void deletePaper(Long id);
}
