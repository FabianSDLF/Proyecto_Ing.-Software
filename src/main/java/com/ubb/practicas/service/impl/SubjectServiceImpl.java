package com.ubb.practicas.service.impl;

import com.ubb.practicas.model.Subject;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final PracticeAssignmentRepository practiceAssignmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Subject> listarAsignaturas() {
        return subjectRepository.findAll();
    }

    @Override
    @Transactional
    public Subject crearAsignatura(Subject subject) {
        subject.setId(null);

        if (subject.getName() == null || subject.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la asignatura es obligatorio");
        }
        if (subject.getName().trim().length() > 255) {
            throw new IllegalArgumentException("El nombre de la asignatura no puede superar los 255 caracteres");
        }
        if (subject.getCycle() == null) {
            throw new IllegalArgumentException("El ciclo de la asignatura es obligatorio");
        }
        subject.setName(subject.getName().trim());

        if (subject.getCode() != null) {
            String trimmedCode = subject.getCode().trim();
            if (trimmedCode.length() > 50) {
                throw new IllegalArgumentException("El código de la asignatura no puede superar los 50 caracteres");
            }
            subject.setCode(trimmedCode.isEmpty() ? null : trimmedCode);
        }

        if (subject.getProfessor() != null) {
            if (subject.getProfessor().getId() == null) {
                subject.setProfessor(null);
            } else if (!userRepository.existsById(subject.getProfessor().getId())) {
                throw new IllegalArgumentException("El profesor especificado no existe");
            }
        }

        try {
            return subjectRepository.save(subject);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Datos de asignatura inválidos o referencia no encontrada", e);
        }
    }

    @Override
    @Transactional
    public void eliminarAsignatura(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new NoSuchElementException("Asignatura no encontrada");
        }
        if (practiceAssignmentRepository != null && practiceAssignmentRepository.existsBySubjectId(id)) {
            throw new IllegalStateException("No se puede eliminar la asignatura porque está asociada a una o más asignaciones de práctica.");
        }
        try {
            subjectRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("No se puede eliminar la asignatura porque tiene dependencias en el sistema.", e);
        }
    }
}
