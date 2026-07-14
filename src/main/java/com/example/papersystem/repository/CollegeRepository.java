package com.example.papersystem.repository;

import com.example.papersystem.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollegeRepository extends JpaRepository<College, Integer> {
    boolean existsByName(String name);
    List<College> findByNameContainingOrderByCreateTimeDesc(String keyword);
    List<College> findAllByOrderByCreateTimeDesc();
}
