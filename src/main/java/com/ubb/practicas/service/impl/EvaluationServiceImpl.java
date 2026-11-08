package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.dto.EvaluationCriteriaDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceConflictException;
import com.ubb.practicas.exception.ResourceNotFoundException;
import com.ubb.practicas.exception.UserNotFoundException;
import com.ubb.practicas.model.Evaluation;
import com.ubb.practicas.model.EvaluationCriteria;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.EvaluationType;
import com.ubb.practicas.repository.EvaluationCriteriaRepository;
import com.ubb.practicas.repository.EvaluationRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationCriteriaRepository evaluationCriteriaRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Evaluation crearEvaluacion(EvaluationCreateDTO dto, String evaluatorEmail) {
        User evaluator = userRepository.findByEmail(evaluatorEmail);
        if (evaluator == null) {
            throw new UserNotFoundException("Evaluador no encontrado");
        }

        if (dto.getPracticeAssignmentId() == null) {
            throw new BadRequestException("El identificador de la asignación de práctica es obligatorio.");
        }

        if (dto.getScore() == null || dto.getScore() < 1.0f || dto.getScore() > 7.0f) {
            throw new BadRequestException("La calificación debe estar comprendida entre 1.0 y 7.0.");
        }

        boolean finalizing = Boolean.TRUE.equals(dto.getIsFinalized());
        if (finalizing && (dto.getDigitalSignature() == null || dto.getDigitalSignature().trim().isEmpty())) {
            throw new BadRequestException("La firma digital es obligatoria para finalizar y registrar oficialmente la evaluación.");
        }

        PracticeAssignment assignment = practiceAssignmentRepository.findById(dto.getPracticeAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada"));

        EvaluationType evalType = dto.getType() != null ? dto.getType() : EvaluationType.PAUTA_OBSERVACION_COLABORADOR;

        Optional<Evaluation> existingOpt = evaluationRepository.findByPracticeAssignmentIdAndType(
                assignment.getId(), evalType
        );

        Evaluation evaluation;
        if (existingOpt.isPresent()) {
            Evaluation existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getIsFinalized())) {
                throw new ResourceConflictException(
                        "Esta evaluación ya fue finalizada y firmada digitalmente; se encuentra registrada como calificación oficial e inamovible en el sistema."
                );
            }
            evaluation = existing;
            evaluationCriteriaRepository.deleteByEvaluationId(existing.getId());
        } else {
            evaluation = new Evaluation();
            evaluation.setPracticeAssignment(assignment);
            evaluation.setCreatedAt(LocalDateTime.now());
        }

        evaluation.setEvaluator(evaluator);
        evaluation.setType(evalType);
        evaluation.setScore(dto.getScore());
        evaluation.setQualitativeComments(dto.getQualitativeComments());
        evaluation.setIsFinalized(finalizing);
        evaluation.setDigitalSignature(finalizing ? dto.getDigitalSignature().trim() : null);
        evaluation.setEvaluationDate(LocalDateTime.now());

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        if (dto.getCriteria() != null && !dto.getCriteria().isEmpty()) {
            for (EvaluationCriteriaDTO c : dto.getCriteria()) {
                EvaluationCriteria criteria = new EvaluationCriteria();
                criteria.setEvaluation(savedEvaluation);
                criteria.setCriteriaName(c.getCriteriaName());
                criteria.setAchievementLevel(c.getAchievementLevel());
                criteria.setPoints(c.getPoints());
                criteria.setJustification(c.getJustification());
                evaluationCriteriaRepository.save(criteria);
            }
        }

        return savedEvaluation;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Evaluation> listarEvaluacionesPorAsignacion(Long practiceAssignmentId) {
        return evaluationRepository.findByPracticeAssignmentId(practiceAssignmentId);
    }
}
