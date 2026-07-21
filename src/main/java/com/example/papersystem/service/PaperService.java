package com.example.papersystem.service;

import com.example.papersystem.entity.Paper;
import org.springframework.data.domain.Page;

public interface PaperService {
    Paper save(Paper paper);
    Paper findById(Long id);
    Page<Paper> findAll(Long studentId, int page, int size, String status, String keyword);
    Paper update(Long id, Paper paper);
    void delete(Long id);
}
