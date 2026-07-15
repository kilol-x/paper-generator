package com.example.papersystem.repository;

import com.example.papersystem.entity.TemplateConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateConfigRepository extends JpaRepository<TemplateConfig, Long> {
    Optional<TemplateConfig> findByTemplateId(Long templateId);
    void deleteByTemplateId(Long templateId);
}
