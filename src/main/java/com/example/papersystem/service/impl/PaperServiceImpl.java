package com.example.papersystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.mapper.PaperMapper;
import com.example.papersystem.service.PaperService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

    @Override
    public Paper createPaper(Paper paper, Long studentId) {
        paper.setStudentId(studentId);
        paper.setCreatedAt(LocalDateTime.now());
        paper.setUpdatedAt(LocalDateTime.now());
        paper.setStatus("DRAFT");
        save(paper);
        return paper;
    }

    @Override
    public Paper getPaperById(Long id) {
        return getById(id);
    }

    @Override
    public List<Paper> getPapersByStudentId(Long studentId) {
        return lambdaQuery().eq(Paper::getStudentId, studentId).list();
    }

    @Override
    public void updatePaperContent(Long id, String content) {
        Paper paper = getById(id);
        paper.setContent(content);
        paper.setUpdatedAt(LocalDateTime.now());
        updateById(paper);
    }

    @Override
    public void deletePaper(Long id) {
        removeById(id);
    }
}