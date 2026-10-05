package com.ubb.practicas.service;

import com.ubb.practicas.model.LessonPlan;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LessonPlanService {
    List<LessonPlan> obtenerMisPlanificaciones(String studentEmail);
    LessonPlan crearPlanificacion(
            String studentEmail,
            Long assignmentId,
            String schoolGrade,
            String mathAxis,
            String objective,
            String specificObjectives,
            String classActivities,
            MultipartFile file
    );
    void eliminarPlanificacion(Long id, String userEmail);
    LessonPlan editarPlanificacion(
            Long id,
            String studentEmail,
            String schoolGrade,
            String mathAxis,
            String objective,
            String specificObjectives,
            String classActivities,
            MultipartFile file
    );
    LessonPlan obtenerPlanificacionPorId(Long id, String userEmail);
    List<LessonPlan> obtenerHistorialPlanificacion(Long id, String userEmail);
}
