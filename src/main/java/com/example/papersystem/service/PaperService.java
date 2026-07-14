package com.example.papersystem.service;

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
}
