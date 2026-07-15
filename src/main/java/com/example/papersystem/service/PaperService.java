package com.example.papersystem.service;

<<<<<<< HEAD
import com.example.papersystem.entity.Paper;

import java.util.List;

public interface PaperService {
    Paper createPaper(Paper paper, Long studentId);
    Paper getPaperById(Long id);
    List<Paper> getPapersByStudentId(Long studentId);
    void updatePaperContent(Long id, String content);
    void deletePaper(Long id);
=======
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.papersystem.entity.Paper;
import com.example.papersystem.mapper.PaperMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaperService {

    @Autowired
    private PaperMapper paperMapper;

    public Paper save(Paper paper) {
        if (paper.getCreatedAt() == null) {
            paper.setCreatedAt(LocalDateTime.now());
        }
        paper.setUpdatedAt(LocalDateTime.now());

        if (paper.getId() == null) {
            paperMapper.insert(paper);
        } else {
            paperMapper.updateById(paper);
        }
        return paper;
    }

    public Paper findById(Long id) {
        return paperMapper.selectById(id);
    }
>>>>>>> 2e14bb3414e7a360b48c444f56eb6df4daf1f733
}
