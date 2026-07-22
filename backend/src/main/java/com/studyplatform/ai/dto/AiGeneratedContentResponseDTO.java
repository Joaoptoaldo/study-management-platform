package com.studyplatform.ai.dto;

import com.studyplatform.ai.ContentType;
import com.studyplatform.ai.DifficultyLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiGeneratedContentResponseDTO {
    private Long id;
    private String title;
    private String content;
    private ContentType type;
    private DifficultyLevel difficulty;
    private LocalDateTime creationDate;
    private Long userId;
    private Long examPrepId;
}
