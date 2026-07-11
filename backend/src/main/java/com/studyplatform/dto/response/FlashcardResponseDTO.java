package com.studyplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardResponseDTO {

    private Long id;
    private String front;
    private String back;
    private LocalDateTime nextReviewDate;
    private Integer box;
    private LocalDateTime creationDate;
    private Long userId;
    private SubjectResponseDTO subject;
    private Long summaryId;
    private String summaryTitle;
}
