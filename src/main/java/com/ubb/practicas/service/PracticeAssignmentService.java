package com.ubb.practicas.service;

import com.ubb.practicas.model.PracticeAssignment;

import java.util.List;

public interface PracticeAssignmentService {
    List<PracticeAssignment> listarAsignaciones();
    List<PracticeAssignment> listarPorColaborador(Long collaboratorId);
    List<PracticeAssignment> listarPorTutor(Long tutorId);
    PracticeAssignment crearAsignacion(PracticeAssignment assignment);
    void eliminarAsignacion(Long id);
    PracticeAssignment resolveActiveAssignment(String studentEmail, Long explicitAssignmentId);
    List<PracticeAssignment> obtenerEstudiantesPorProfesor(Long professorId, String callerEmail);
}
