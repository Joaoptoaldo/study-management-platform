package com.studyplatform.mapper;

import com.studyplatform.dto.request.SummaryRequestDTO;
import com.studyplatform.dto.response.SummaryResponseDTO;
import com.studyplatform.entity.Summary;
import com.studyplatform.entity.Subject;
import com.studyplatform.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummaryMapper {

    private final SubjectMapper subjectMapper;

    public Summary toEntity(SummaryRequestDTO dto, User user, Subject subject) {
        return Summary.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .user(user)
                .subject(subject)
                .build();
    }

    public SummaryResponseDTO toResponseDTO(Summary summary) {
        return SummaryResponseDTO.builder()
                .id(summary.getId())
                .title(summary.getTitle())
                .content(summary.getContent())
                .creationDate(summary.getCreationDate())
                .lastModifiedDate(summary.getLastModifiedDate())
                .userId(summary.getUser().getId())
                .userName(summary.getUser().getNameUser())
                .subject(summary.getSubject() != null ? subjectMapper.toResponseDTO(summary.getSubject()) : null)
                .build();
    }

    public void updateEntityFromDTO(Summary summary, SummaryRequestDTO dto, Subject subject) {
        summary.setTitle(dto.getTitle());
        summary.setContent(dto.getContent());
        summary.setSubject(subject);
    }
}
