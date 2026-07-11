package com.studyplatform.service;

import com.studyplatform.dto.request.SummaryRequestDTO;
import com.studyplatform.dto.response.SummaryResponseDTO;
import com.studyplatform.entity.Summary;
import com.studyplatform.entity.Subject;
import com.studyplatform.entity.User;
import com.studyplatform.exception.ResourceNotFoundException;
import com.studyplatform.mapper.SummaryMapper;
import com.studyplatform.repository.SummaryRepository;
import com.studyplatform.repository.SubjectRepository;
import com.studyplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final SummaryMapper summaryMapper;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }

    private Subject findSubjectByIdAndUser(Long subjectId, Long userId) {
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matéria não encontrada com o id: " + subjectId));
    }

    @Transactional(readOnly = true)
    public List<SummaryResponseDTO> findAll() {
        User user = getAuthenticatedUser();
        return summaryRepository.findByUserId(user.getId())
                .stream()
                .map(summaryMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SummaryResponseDTO findById(Long id) {
        User user = getAuthenticatedUser();
        Summary summary = summaryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resumo não encontrado com o id: " + id));
        return summaryMapper.toResponseDTO(summary);
    }

    @Transactional(readOnly = true)
    public List<SummaryResponseDTO> findBySubjectId(Long subjectId) {
        User user = getAuthenticatedUser();
        // Valida se a matéria existe e pertence ao usuário
        findSubjectByIdAndUser(subjectId, user.getId());
        
        return summaryRepository.findByUserIdAndSubjectId(user.getId(), subjectId)
                .stream()
                .map(summaryMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public SummaryResponseDTO create(SummaryRequestDTO request) {
        User user = getAuthenticatedUser();
        Subject subject = findSubjectByIdAndUser(request.getSubjectId(), user.getId());

        Summary summary = summaryMapper.toEntity(request, user, subject);
        Summary savedSummary = summaryRepository.save(summary);

        return summaryMapper.toResponseDTO(savedSummary);
    }

    @Transactional
    public SummaryResponseDTO update(Long id, SummaryRequestDTO request) {
        User user = getAuthenticatedUser();
        Summary summary = summaryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resumo não encontrado com o id: " + id));

        Subject subject = findSubjectByIdAndUser(request.getSubjectId(), user.getId());

        summaryMapper.updateEntityFromDTO(summary, request, subject);
        Summary updatedSummary = summaryRepository.save(summary);

        return summaryMapper.toResponseDTO(updatedSummary);
    }

    @Transactional
    public void delete(Long id) {
        User user = getAuthenticatedUser();
        Summary summary = summaryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Resumo não encontrado com o id: " + id));

        summaryRepository.delete(summary);
    }
}
