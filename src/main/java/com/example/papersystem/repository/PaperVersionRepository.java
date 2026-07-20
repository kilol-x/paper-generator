package com.example.papersystem.repository;
import com.example.papersystem.entity.PaperVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PaperVersionRepository extends JpaRepository<PaperVersion, Long> {
    List<PaperVersion> findByPaperIdOrderByVersionNoDescCreatedAtDesc(Long paperId);
}
