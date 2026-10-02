package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.dto.EvaluationCriteriaDTO;
import com.ubb.practicas.model.Evaluation;
import com.ubb.practicas.model.EvaluationCriteria;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.User;
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
            throw new RuntimeException("Evaluador no encontrado");
        }

        PracticeAssignment assignment = practiceAssignmentRepository.findById(dto.getPracticeAssignmentId())
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));

        Evaluation evaluation = new Evaluation();
        evaluation.setPracticeAssignment(assignment);
        evaluation.setEvaluator(evaluator);
        evaluation.setType(dto.getType());
        evaluation.setScore(dto.getScore());
        evaluation.setQualitativeComments(dto.getQualitativeComments());
        evaluation.setEvaluationDate(LocalDateTime.now());
        evaluation.setCreatedAt(LocalDateTime.now());

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
