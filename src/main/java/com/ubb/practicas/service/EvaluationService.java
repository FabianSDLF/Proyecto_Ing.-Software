package com.ubb.practicas.service;

import com.ubb.practicas.dto.EvaluationCreateDTO;
import com.ubb.practicas.dto.EvaluationFinalizeDTO;
import com.ubb.practicas.model.Evaluation;
import java.util.List;

public interface EvaluationService {
    Evaluation crearEvaluacion(EvaluationCreateDTO dto, String evaluatorEmail);
    Evaluation finalizarEvaluacion(Long evaluationId, EvaluationFinalizeDTO dto, String evaluatorEmail);
    Evaluation obtenerEvaluacionPorId(Long evaluationId, String callerEmail);
    List<Evaluation> listarEvaluacionesPorAsignacion(Long practiceAssignmentId);
    List<Evaluation> listarEvaluacionesPorAsignacion(Long practiceAssignmentId, String callerEmail);
}
