package com.ubb.practicas.service;

import com.ubb.practicas.dto.StudentPairAssignmentSummaryDTO;
import com.ubb.practicas.dto.StudentPairCreateRequest;
import com.ubb.practicas.dto.StudentPairDTO;

import java.util.List;

public interface StudentPairService {

    /**
     * Creates and registers a new student pair for intermediate cycle assignments.
     * Enforces all business rules, single pairing per period, and immutability.
     */
    StudentPairDTO crearDupla(StudentPairCreateRequest request);

    /**
     * Retrieves all pairs for a specific academic period, or all pairs if periodId is null.
     */
    List<StudentPairDTO> listarDuplasPorPeriodo(Long periodId);

    /**
     * Lists all intermediate cycle assignments in the given period that do not belong to any pair.
     */
    List<StudentPairAssignmentSummaryDTO> listarAsignacionesDisponibles(Long periodId);

    /**
     * Retrieves a pair by its ID.
     */
    StudentPairDTO obtenerDuplaPorId(Long id);
}
