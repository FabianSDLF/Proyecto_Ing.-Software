package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Alcance de consultas: la identidad siempre se obtiene del principal autenticado. */
@Service
@RequiredArgsConstructor
public class AssignmentScope {
    private final PracticeAssignmentRepository assignments;
    private final UserRepository users;

    public User user(String email) {
        User user = users.findByEmail(email);
        if (user == null || !Boolean.TRUE.equals(user.getActive())) throw new AccessDeniedException("Cuenta no autorizada");
        return user;
    }

    public static String role(User user) {
        return user.getRole() == null ? "" : user.getRole().getName().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    public static boolean isAdmin(User user) {
        return Set.of("ADMINISTRADOR", "COORDINADOR_DE_PRACTICAS").contains(role(user));
    }

    public static boolean current(PracticeAssignment a) {
        return a.getStatus() == AssignmentStatus.ACTIVA
                && (a.getPeriod() == null || a.getPeriod().getStatus() == PeriodStatus.ACTIVO);
    }

    @Transactional(readOnly = true)
    public List<PracticeAssignment> visible(String email, Long periodId, boolean history) {
        User user = user(email);
        List<PracticeAssignment> list;
        if (isAdmin(user)) list = assignments.findAll();
        else list = switch (role(user)) {
            case "PROFESOR_DE_ASIGNATURA" -> assignments.findBySubjectProfessorId(user.getId());
            case "PROFESOR_COLABORADOR" -> assignments.findByCollaboratorId(user.getId());
            case "TUTOR" -> assignments.findByTutorId(user.getId());
            case "ESTUDIANTE" -> assignments.findByStudentEmail(email);
            default -> throw new AccessDeniedException("Rol sin acceso a prácticas");
        };
        return list.stream().filter(a -> periodId != null
                ? a.getPeriod() != null && periodId.equals(a.getPeriod().getId())
                : history || current(a)).toList();
    }

    @Transactional(readOnly = true)
    public PracticeAssignment require(Long id, String email) {
        User user = user(email);
        PracticeAssignment a = assignments.findById(id).orElseThrow(() -> new NoSuchElementException("Práctica no encontrada"));
        if (!isAdmin(user) && !a.hasParticipant(email)) throw new AccessDeniedException("No pertenece a esta práctica");
        return a;
    }

    public void requireExport(String email) {
        User user = user(email);
        if (!isAdmin(user) && !Set.of("PROFESOR_DE_ASIGNATURA", "PROFESOR_COLABORADOR", "TUTOR").contains(role(user)))
            throw new AccessDeniedException("Solo autoridades y docentes pueden exportar resultados");
    }
}
