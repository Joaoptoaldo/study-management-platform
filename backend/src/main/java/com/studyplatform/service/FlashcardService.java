package com.studyplatform.service;

import com.studyplatform.dto.request.FlashcardRequestDTO;
import com.studyplatform.dto.response.FlashcardResponseDTO;
import com.studyplatform.entity.Flashcard;
import com.studyplatform.entity.Subject;
import com.studyplatform.entity.Summary;
import com.studyplatform.entity.User;
import com.studyplatform.exception.ResourceNotFoundException;
import com.studyplatform.mapper.FlashcardMapper;
import com.studyplatform.repository.FlashcardRepository;
import com.studyplatform.repository.SubjectRepository;
import com.studyplatform.repository.SummaryRepository;
import com.studyplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final SubjectRepository subjectRepository;
    private final SummaryRepository summaryRepository;
    private final UserRepository userRepository;
    private final FlashcardMapper flashcardMapper;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }

    private Subject findSubjectByIdAndUser(Long subjectId, Long userId) {
        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Matéria não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponseDTO> findAll() {
        User user = getAuthenticatedUser();
        return flashcardRepository.findByUserId(user.getId())
                .stream()
                .map(flashcardMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponseDTO> findDue() {
        User user = getAuthenticatedUser();
        return flashcardRepository.findByUserIdAndNextReviewDateBefore(user.getId(), LocalDateTime.now())
                .stream()
                .map(flashcardMapper::toResponseDTO)
                .toList();
    }

    @Transactional
    public FlashcardResponseDTO create(FlashcardRequestDTO request) {
        User user = getAuthenticatedUser();
        Subject subject = findSubjectByIdAndUser(request.getSubjectId(), user.getId());
        
        Summary summary = null;
        if (request.getSummaryId() != null) {
            summary = summaryRepository.findByIdAndUserId(request.getSummaryId(), user.getId())
                    .orElse(null);
        }

        Flashcard flashcard = flashcardMapper.toEntity(request, user, subject, summary);
        Flashcard saved = flashcardRepository.save(flashcard);
        return flashcardMapper.toResponseDTO(saved);
    }

    @Transactional
    public FlashcardResponseDTO update(Long id, FlashcardRequestDTO request) {
        User user = getAuthenticatedUser();
        Flashcard flashcard = flashcardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));

        Subject subject = findSubjectByIdAndUser(request.getSubjectId(), user.getId());
        
        Summary summary = null;
        if (request.getSummaryId() != null) {
            summary = summaryRepository.findByIdAndUserId(request.getSummaryId(), user.getId())
                    .orElse(null);
        }

        flashcardMapper.updateEntityFromDTO(flashcard, request, subject, summary);
        Flashcard updated = flashcardRepository.save(flashcard);
        return flashcardMapper.toResponseDTO(updated);
    }

    @Transactional
    public FlashcardResponseDTO review(Long id, String quality) {
        User user = getAuthenticatedUser();
        Flashcard flashcard = flashcardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));

        int currentBox = flashcard.getBox();
        int nextBox = currentBox;
        int intervalDays = 1;

        if ("easy".equalsIgnoreCase(quality) || "good".equalsIgnoreCase(quality)) {
            nextBox = Math.min(5, currentBox + 1);
            switch (nextBox) {
                case 1: intervalDays = 1; break;
                case 2: intervalDays = 3; break;
                case 3: intervalDays = 7; break;
                case 4: intervalDays = 14; break;
                case 5: intervalDays = 30; break;
            }
        } else {
            nextBox = 1;
            intervalDays = 1;
        }

        flashcard.setBox(nextBox);
        flashcard.setNextReviewDate(LocalDateTime.now().plusDays(intervalDays));
        
        Flashcard updated = flashcardRepository.save(flashcard);
        return flashcardMapper.toResponseDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        User user = getAuthenticatedUser();
        Flashcard flashcard = flashcardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cartão não encontrado"));
        flashcardRepository.delete(flashcard);
    }
}
