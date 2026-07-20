package com.example.papersystem.service.impl;

import com.example.papersystem.entity.Paper;
import com.example.papersystem.repository.PaperRepository;
import com.example.papersystem.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaperServiceImpl implements PaperService {

    @Autowired
    private PaperRepository paperRepository;

    @Override
    public Paper save(Paper paper) {
        return paperRepository.save(paper);
    }

    @Override
    public Paper findById(Long id) {
        return paperRepository.findById(id).orElse(null);
    }
}