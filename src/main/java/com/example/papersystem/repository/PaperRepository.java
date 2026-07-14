package com.example.papersystem.repository;

import com.example.papersystem.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Long> {
    List<Paper> findByStudentIdOrderByUpdatedAtDesc(Long studentId);
}
