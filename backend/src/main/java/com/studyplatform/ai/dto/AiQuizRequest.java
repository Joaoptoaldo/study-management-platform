package com.studyplatform.ai.dto;

import com.studyplatform.ai.DifficultyLevel;
import lombok.Data;
import java.util.List;

@Data
public class AiQuizRequest {
    private Long examPrepId;
    private List<String> chunks;
    private DifficultyLevel difficulty;
}
