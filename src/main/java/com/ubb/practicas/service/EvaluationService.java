package com.ubb.practicas.service;

import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.model.Evaluation;
import java.util.List;

public interface EvaluationService {
    Evaluation crearEvaluacion(EvaluationCreateDTO dto, String evaluatorEmail);
    List<Evaluation> listarEvaluacionesPorAsignacion(Long practiceAssignmentId);
}
