package com.studyplatform.repository;

import com.studyplatform.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {

    List<Flashcard> findByUserId(Long userId);

    Optional<Flashcard> findByIdAndUserId(Long id, Long userId);

    List<Flashcard> findByUserIdAndSubjectId(Long userId, Long subjectId);

    List<Flashcard> findByUserIdAndNextReviewDateBefore(Long userId, LocalDateTime date);
}
