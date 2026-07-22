package com.studyplatform.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiSummaryRequest {
    private Long examPrepId;
    private List<String> chunks;
}
