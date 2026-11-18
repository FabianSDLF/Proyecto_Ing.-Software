package com.ubb.practicas.service;

import com.ubb.practicas.model.*;
import com.ubb.practicas.model.enums.*;
import com.ubb.practicas.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompletedHuScopeTest {
    @Mock PracticeAssignmentRepository assignments;
    @Mock UserRepository users;
    @InjectMocks AssignmentScope scope;
    User student;
    PracticeAssignment current;
    PracticeAssignment archived;

    @BeforeEach void setup() {
        student = new User(); student.setId(1L); student.setEmail("student@example.test"); student.setActive(true);
        Role role = new Role(); role.setName("Estudiante"); student.setRole(role);
        current = new PracticeAssignment(); current.setId(10L); current.setStudent(student); current.setStatus(AssignmentStatus.ACTIVA);
        AcademicPeriod period = new AcademicPeriod(); period.setId(1L); period.setStatus(PeriodStatus.ACTIVO); current.setPeriod(period);
        archived = new PracticeAssignment(); archived.setId(11L); archived.setStudent(student); archived.setStatus(AssignmentStatus.ACTIVA);
        AcademicPeriod closed = new AcademicPeriod(); closed.setId(2L); closed.setStatus(PeriodStatus.CERRADO); archived.setPeriod(closed);
        when(users.findByEmail(student.getEmail())).thenReturn(student);
    }

    @Test void hidesArchivedPeriodsButRetainsExplicitHistory() {
        when(assignments.findByStudentEmail(student.getEmail())).thenReturn(List.of(current, archived));
        assertEquals(List.of(current), scope.visible(student.getEmail(), null, false));
        assertEquals(List.of(current, archived), scope.visible(student.getEmail(), null, true));
        assertEquals(List.of(archived), scope.visible(student.getEmail(), 2L, false));
    }

    @Test void fichaCannotReadOtherStudentsPractice() {
        User other = new User(); other.setEmail("other@example.test"); archived.setStudent(other);
        when(assignments.findById(11L)).thenReturn(Optional.of(archived));
        assertThrows(AccessDeniedException.class, () -> scope.require(11L, student.getEmail()));
    }

    @Test void studentCannotExportInstitutionalData() {
        assertThrows(AccessDeniedException.class, () -> scope.requireExport(student.getEmail()));
    }
}
