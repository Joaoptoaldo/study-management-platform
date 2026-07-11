package com.studyplatform.repository;

import com.studyplatform.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    List<Summary> findByUserId(Long userId);

    Optional<Summary> findByIdAndUserId(Long id, Long userId);

    List<Summary> findByUserIdAndSubjectId(Long userId, Long subjectId);
}
