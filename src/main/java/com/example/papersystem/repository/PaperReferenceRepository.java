package com.example.papersystem.repository;

import com.example.papersystem.entity.PaperReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperReferenceRepository extends JpaRepository<PaperReference, Long> {
    List<PaperReference> findByPaperIdOrderByIndexAsc(Long paperId);
}