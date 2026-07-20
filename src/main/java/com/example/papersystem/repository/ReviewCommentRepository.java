package com.example.papersystem.repository;
import com.example.papersystem.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByPaperIdOrderByCreatedAtAsc(Long paperId);
}
