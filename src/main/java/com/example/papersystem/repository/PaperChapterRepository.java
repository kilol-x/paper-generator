package com.example.papersystem.repository;

import com.example.papersystem.entity.PaperChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperChapterRepository extends JpaRepository<PaperChapter, Long> {
    List<PaperChapter> findByPaperIdOrderBySortOrderAsc(Long paperId);
}