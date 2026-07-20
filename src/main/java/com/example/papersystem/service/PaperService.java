package com.example.papersystem.service;

import com.example.papersystem.entity.Paper;

public interface PaperService {
    Paper save(Paper paper);
    Paper findById(Long id);
}