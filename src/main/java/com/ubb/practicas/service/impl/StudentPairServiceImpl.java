package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.StudentPairAssignmentSummaryDTO;
import com.ubb.practicas.dto.StudentPairCreateRequest;
import com.ubb.practicas.dto.StudentPairDTO;
import com.ubb.practicas.exception.BadRequestException;
import com.ubb.practicas.exception.ResourceNotFoundException;
import com.ubb.practicas.model.AcademicPeriod;
import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.model.StudentPair;
import com.ubb.practicas.model.enums.AssignmentStatus;
import com.ubb.practicas.model.enums.PeriodStatus;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.AcademicPeriodRepository;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.StudentPairRepository;
import com.ubb.practicas.service.StudentPairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentPairServiceImpl implements StudentPairService {

    private final StudentPairRepository studentPairRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;

    @Override
    @Transactional
    public StudentPairDTO crearDupla(StudentPairCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Los datos de la dupla no pueden ser nulos.");
        }
        if (request.getAssignment1Id() == null || request.getAssignment2Id() == null || request.getPeriodId() == null) {
            throw new BadRequestException("Los identificadores de ambas asignaciones y del período son obligatorios.");
        }
        if (request.getAssignment1Id() <= 0 || request.getAssignment2Id() <= 0 || request.getPeriodId() <= 0) {
            throw new BadRequestException("Los identificadores deben ser válidos y positivos.");
        }

        // Rule 1: Anti-self-pairing (assignment IDs cannot match)
        if (request.getAssignment1Id().equals(request.getAssignment2Id())) {
            throw new BadRequestException("No se puede crear una dupla emparejando a una asignación consigo misma.");
        }

        // Rule 2: Existence checks
        PracticeAssignment assignment1 = practiceAssignmentRepository.findById(request.getAssignment1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Asignación de práctica no encontrada con ID: " + request.getAssignment1Id()));

        PracticeAssignment assignment2 = practiceAssignmentRepository.findById(request.getAssignment2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Asignación de práctica no encontrada con ID: " + request.getAssignment2Id()));

        AcademicPeriod period = academicPeriodRepository.findById(request.getPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Período académico no encontrado con ID: " + request.getPeriodId()));

        // Rule 3: Anti-self-student pairing
        if (assignment1.getStudent() == null || assignment2.getStudent() == null) {
            throw new BadRequestException("Ambas asignaciones deben tener un estudiante asociado.");
        }
        if (assignment1.getStudent().getId().equals(assignment2.getStudent().getId())) {
            throw new BadRequestException("No se puede crear una dupla con el mismo estudiante.");
        }

        // Rule 4: Active status check
        if (assignment1.getStatus() != AssignmentStatus.ACTIVA) {
            throw new BadRequestException("La primera asignación no se encuentra activa (estado actual: " + assignment1.getStatus() + ").");
        }
        if (assignment2.getStatus() != AssignmentStatus.ACTIVA) {
            throw new BadRequestException("La segunda asignación no se encuentra activa (estado actual: " + assignment2.getStatus() + ").");
        }

        // Rule 5: Cycle validation (strictly INTERMEDIO for both)
        if (assignment1.getSubject() == null || assignment1.getSubject().getCycle() != PracticeCycle.INTERMEDIO) {
            throw new BadRequestException("La asignación 1 no corresponde al ciclo Intermedio. Las duplas solo están permitidas para el Ciclo Intermedio.");
        }
        if (assignment2.getSubject() == null || assignment2.getSubject().getCycle() != PracticeCycle.INTERMEDIO) {
            throw new BadRequestException("La asignación 2 no corresponde al ciclo Intermedio. Las duplas solo están permitidas para el Ciclo Intermedio.");
        }

        // Rule 6: Cohort / Period validation
        if (assignment1.getPeriod() == null || !assignment1.getPeriod().getId().equals(request.getPeriodId())) {
            throw new BadRequestException("La asignación 1 no pertenece al período académico especificado (ID: " + request.getPeriodId() + ").");
        }
        if (assignment2.getPeriod() == null || !assignment2.getPeriod().getId().equals(request.getPeriodId())) {
            throw new BadRequestException("La asignación 2 no pertenece al período académico especificado (ID: " + request.getPeriodId() + ").");
        }

        // Rule 7: Single-pair uniqueness check in current period
        if (studentPairRepository.existsByAssignmentIdAndPeriodId(assignment1.getId(), request.getPeriodId())
                || studentPairRepository.existsByStudentIdAndPeriodId(assignment1.getStudent().getId(), request.getPeriodId())) {
            throw new BadRequestException("El estudiante '" + assignment1.getStudent().getFullName() + "' ya tiene una dupla asignada en este período.");
        }

        if (studentPairRepository.existsByAssignmentIdAndPeriodId(assignment2.getId(), request.getPeriodId())
                || studentPairRepository.existsByStudentIdAndPeriodId(assignment2.getStudent().getId(), request.getPeriodId())) {
            throw new BadRequestException("El estudiante '" + assignment2.getStudent().getFullName() + "' ya tiene una dupla asignada en este período.");
        }

        // Persist Pair
        StudentPair pair = new StudentPair();
        pair.setAssignment1(assignment1);
        pair.setAssignment2(assignment2);
        pair.setPeriod(period);
        pair.setCreatedAt(LocalDateTime.now());

        StudentPair saved = studentPairRepository.save(pair);
        log.info("Dupla creada exitosamente [ID: {}] entre estudiante {} y estudiante {} para el período {}",
                saved.getId(), assignment1.getStudent().getEmail(), assignment2.getStudent().getEmail(), period.getName());

        return StudentPairDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentPairDTO> listarDuplasPorPeriodo(Long periodId) {
        List<StudentPair> pairs;
        if (periodId != null) {
            pairs = studentPairRepository.findByPeriodId(periodId);
        } else {
            pairs = studentPairRepository.findAll();
        }
        return pairs.stream().map(StudentPairDTO::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentPairAssignmentSummaryDTO> listarAsignacionesDisponibles(Long periodId) {
        Long targetPeriodId = periodId;
        if (targetPeriodId == null) {
            targetPeriodId = academicPeriodRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PeriodStatus.ACTIVO)
                    .map(AcademicPeriod::getId)
                    .findFirst()
                    .orElseGet(() -> academicPeriodRepository.findAll().stream()
                            .map(AcademicPeriod::getId)
                            .findFirst()
                            .orElse(null));
        }

        if (targetPeriodId == null) {
            throw new BadRequestException("El parámetro periodId es obligatorio para consultar estudiantes disponibles.");
        }
        if (!academicPeriodRepository.existsById(targetPeriodId)) {
            throw new ResourceNotFoundException("Período académico no encontrado con ID: " + targetPeriodId);
        }

        List<PracticeAssignment> available = studentPairRepository.findAvailableUnpairedIntermediateAssignments(targetPeriodId);
        return available.stream().map(StudentPairAssignmentSummaryDTO::fromAssignment).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentPairDTO obtenerDuplaPorId(Long id) {
        StudentPair pair = studentPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dupla no encontrada con ID: " + id));
        return StudentPairDTO.fromEntity(pair);
    }
}
