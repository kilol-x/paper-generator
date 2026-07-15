package com.example.papersystem.repository;

import com.example.papersystem.entity.Template;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long>, JpaSpecificationExecutor<Template> {

    static Specification<Template> filter(Integer collegeId, String type, Integer status) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();
            if (collegeId != null) ps.add(cb.equal(root.get("collegeId"), collegeId));
            if (type != null && !type.isEmpty()) ps.add(cb.equal(root.get("type"), type));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            query.orderBy(cb.desc(root.get("updateTime")));
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
