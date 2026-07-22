package com.example.papersystem.repository;

import com.example.papersystem.entity.ReferenceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferenceEntryRepository extends JpaRepository<ReferenceEntry, Long> {
    List<ReferenceEntry> findByPaperIdOrderByCitationNoAscCreatedAtAsc(Long paperId);

    long countByPaperId(Long paperId);
}