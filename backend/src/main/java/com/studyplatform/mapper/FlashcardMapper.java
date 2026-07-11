package com.studyplatform.mapper;

import com.studyplatform.dto.request.FlashcardRequestDTO;
import com.studyplatform.dto.response.FlashcardResponseDTO;
import com.studyplatform.entity.Flashcard;
import com.studyplatform.entity.Subject;
import com.studyplatform.entity.Summary;
import com.studyplatform.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FlashcardMapper {

    private final SubjectMapper subjectMapper;

    public Flashcard toEntity(FlashcardRequestDTO dto, User user, Subject subject, Summary summary) {
        return Flashcard.builder()
                .front(dto.getFront())
                .back(dto.getBack())
                .nextReviewDate(LocalDateTime.now())
                .box(1)
                .user(user)
                .subject(subject)
                .summary(summary)
                .build();
    }

    public FlashcardResponseDTO toResponseDTO(Flashcard flashcard) {
        return FlashcardResponseDTO.builder()
                .id(flashcard.getId())
                .front(flashcard.getFront())
                .back(flashcard.getBack())
                .nextReviewDate(flashcard.getNextReviewDate())
                .box(flashcard.getBox())
                .creationDate(flashcard.getCreationDate())
                .userId(flashcard.getUser().getId())
                .subject(flashcard.getSubject() != null ? subjectMapper.toResponseDTO(flashcard.getSubject()) : null)
                .summaryId(flashcard.getSummary() != null ? flashcard.getSummary().getId() : null)
                .summaryTitle(flashcard.getSummary() != null ? flashcard.getSummary().getTitle() : null)
                .build();
    }

    public void updateEntityFromDTO(Flashcard flashcard, FlashcardRequestDTO dto, Subject subject, Summary summary) {
        flashcard.setFront(dto.getFront());
        flashcard.setBack(dto.getBack());
        flashcard.setSubject(subject);
        flashcard.setSummary(summary);
    }
}
