package com.studyplatform.service;

import com.studyplatform.dto.request.GoalRequestDTO;
import com.studyplatform.dto.response.GoalResponseDTO;
import com.studyplatform.entity.Goal;
import com.studyplatform.entity.Subject;
import com.studyplatform.entity.User;
import com.studyplatform.exception.BusinessException;
import com.studyplatform.exception.ResourceNotFoundException;
import com.studyplatform.mapper.GoalMapper;
import com.studyplatform.repository.GoalRepository;
import com.studyplatform.repository.SubjectRepository;
import com.studyplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final GoalMapper goalMapper;
    private final com.studyplatform.repository.StudySessionRepository studySessionRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
    }

    // Retorna null se subjectId for null (meta geral), ou valida o ownership se informado.
    private Subject resolveSubject(Long subjectId, Long userId) {
        if (subjectId == null) {
            return null;
        }

        return subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Matéria não encontrada com o id: " + subjectId));
    }

    private void validateGoalRules(GoalRequestDTO request) {
        if (request.getEndDateGoal().isBefore(request.getStartDateGoal())) {
            throw new BusinessException("A data de término deve ser posterior à data de início");
        }
    }

    private double obterProgressoCalculado(Long userId, Long subjectId, java.time.LocalDate start, java.time.LocalDate end) {
        Integer totalMins;
        if (subjectId != null) {
            totalMins = studySessionRepository.sumDurationBySubjectAndPeriod(userId, subjectId, start, end);
        } else {
            totalMins = studySessionRepository.sumDurationByUserAndPeriod(userId, start, end);
        }
        double progressHours = totalMins / 60.0;
        return Math.round(progressHours * 100.0) / 100.0;
    }

    @Transactional
    public void recalcularMetasDoUsuarioNoPeriodo(Long userId, java.time.LocalDate dataSessao) {
        List<Goal> metasAfetadas = goalRepository.findByUserId(userId)
                .stream()
                .filter(g -> !dataSessao.isBefore(g.getStartDateGoal()) && !dataSessao.isAfter(g.getEndDateGoal()))
                .toList();

        for (Goal goal : metasAfetadas) {
            double progresso = obterProgressoCalculado(
                    userId,
                    goal.getSubject() != null ? goal.getSubject().getId() : null,
                    goal.getStartDateGoal(),
                    goal.getEndDateGoal()
            );
            goal.setProgress(progresso);
            goalRepository.save(goal);
        }
    }

    @Transactional(readOnly = true)
    public List<GoalResponseDTO> findAll() {
        User user = getAuthenticatedUser();

        return goalRepository.findByUserId(user.getId())
                .stream()
                .map(goalMapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalResponseDTO findById(Long id) {
        User user = getAuthenticatedUser();

        Goal goal = goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meta não encontrada com o id: " + id));

        return goalMapper.toResponseDTO(goal);
    }

    @Transactional
    public GoalResponseDTO create(GoalRequestDTO request) {
        User user = getAuthenticatedUser();
        validateGoalRules(request);

        Subject subject = resolveSubject(request.getSubjectId(), user.getId());
        Goal goal = goalMapper.toEntity(request, user, subject);
        
        double progresso = obterProgressoCalculado(user.getId(), subject != null ? subject.getId() : null, request.getStartDateGoal(), request.getEndDateGoal());
        goal.setProgress(progresso);
        
        Goal savedGoal = goalRepository.save(goal);

        return goalMapper.toResponseDTO(savedGoal);
    }

    @Transactional
    public GoalResponseDTO update(Long id, GoalRequestDTO request) {
        User user = getAuthenticatedUser();

        Goal goal = goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meta não encontrada com o id: " + id));

        validateGoalRules(request);

        Subject subject = resolveSubject(request.getSubjectId(), user.getId());
        goalMapper.updateEntityFromDTO(goal, request, subject);
        
        double progresso = obterProgressoCalculado(user.getId(), subject != null ? subject.getId() : null, request.getStartDateGoal(), request.getEndDateGoal());
        goal.setProgress(progresso);
        
        Goal updatedGoal = goalRepository.save(goal);

        return goalMapper.toResponseDTO(updatedGoal);
    }

    @Transactional
    public void delete(Long id) {
        User user = getAuthenticatedUser();

        Goal goal = goalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Meta não encontrada com o id: " + id));

        goalRepository.delete(goal);
    }
}
