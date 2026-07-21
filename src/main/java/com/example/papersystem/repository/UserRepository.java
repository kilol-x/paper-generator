package com.example.papersystem.repository;

import com.example.papersystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByTeacherIdAndRoleOrderByCreateTimeDesc(Long teacherId, String role);
}
