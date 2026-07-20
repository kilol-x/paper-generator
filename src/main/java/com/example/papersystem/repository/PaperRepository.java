package com.example.papersystem.repository;

import com.example.papersystem.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    List<Paper> findByStudentIdOrderByUpdatedAtDesc(Long studentId);

    Page<Paper> findByStudentIdOrderByUpdatedAtDesc(Long studentId, Pageable pageable);

    @Query("SELECT p FROM Paper p WHERE p.studentId = :studentId " +
           "AND (:status IS NULL OR :status = '' OR p.status = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Paper> findPapers(@Param("studentId") Long studentId,
                           @Param("status") String status,
                           @Param("keyword") String keyword,
                           Pageable pageable);
}
