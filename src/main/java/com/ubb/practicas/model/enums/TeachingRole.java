package com.ubb.practicas.model.enums;

import com.ubb.practicas.model.PracticeAssignment;
import com.ubb.practicas.repository.PracticeAssignmentRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public enum TeachingRole {
    PROFESOR_ASIGNATURA("Profesor de Asignatura", PracticeAssignmentRepository::findBySubjectProfessorId),
    PROFESOR_COLABORADOR("Profesor Colaborador", PracticeAssignmentRepository::findByCollaboratorId),
    TUTOR("Tutor", PracticeAssignmentRepository::findByTutorId);

    private final String roleName;
    private final BiFunction<PracticeAssignmentRepository, Long, List<PracticeAssignment>> assignmentFetcher;

    TeachingRole(String roleName, BiFunction<PracticeAssignmentRepository, Long, List<PracticeAssignment>> assignmentFetcher) {
        this.roleName = roleName;
        this.assignmentFetcher = assignmentFetcher;
    }

    public String getRoleName() {
        return roleName;
    }

    public static Optional<TeachingRole> fromRoleName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(r -> r.roleName.equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    public List<PracticeAssignment> findAssignments(PracticeAssignmentRepository repository, Long professorId) {
        return this.assignmentFetcher.apply(repository, professorId);
    }
}
