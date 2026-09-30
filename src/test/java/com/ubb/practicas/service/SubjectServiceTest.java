package com.ubb.practicas.service;

import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.User;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.PracticeAssignmentRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.repository.UserRepository;
import com.ubb.practicas.service.impl.SubjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private PracticeAssignmentRepository practiceAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubjectServiceImpl subjectService;

    @Test
    void crearAsignatura_Success() {
        Subject subject = new Subject();
        subject.setId(99L);
        subject.setName("  Geometría Descriptiva  ");
        subject.setCode("  GEO-101  ");
        subject.setCycle(PracticeCycle.INICIAL);

        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        Subject result = subjectService.crearAsignatura(subject);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals("Geometría Descriptiva", result.getName());
        assertEquals("GEO-101", result.getCode());
        assertEquals(PracticeCycle.INICIAL, result.getCycle());
        verify(subjectRepository).save(subject);
    }

    @Test
    void crearAsignatura_NullProfessorId_ResetsProfessorToNull() {
        Subject subject = new Subject();
        subject.setName("Álgebra Lineal");
        subject.setCycle(PracticeCycle.INTERMEDIO);
        User prof = new User();
        prof.setId(null);
        subject.setProfessor(prof);

        when(subjectRepository.save(any(Subject.class))).thenAnswer(inv -> inv.getArgument(0));

        Subject result = subjectService.crearAsignatura(subject);

        assertNotNull(result);
        assertNull(result.getProfessor());
        verify(subjectRepository).save(subject);
    }

    @Test
    void crearAsignatura_BlankName_ThrowsIllegalArgumentException() {
        Subject subject = new Subject();
        subject.setName("   ");
        subject.setCycle(PracticeCycle.INICIAL);

        assertThrows(IllegalArgumentException.class, () -> subjectService.crearAsignatura(subject));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void crearAsignatura_NameExceeds255Chars_ThrowsIllegalArgumentException() {
        Subject subject = new Subject();
        subject.setName("A".repeat(256));
        subject.setCycle(PracticeCycle.INICIAL);

        assertThrows(IllegalArgumentException.class, () -> subjectService.crearAsignatura(subject));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void crearAsignatura_NullCycle_ThrowsIllegalArgumentException() {
        Subject subject = new Subject();
        subject.setName("Cálculo I");
        subject.setCycle(null);

        assertThrows(IllegalArgumentException.class, () -> subjectService.crearAsignatura(subject));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void crearAsignatura_CodeExceeds50Chars_ThrowsIllegalArgumentException() {
        Subject subject = new Subject();
        subject.setName("Estadística");
        subject.setCycle(PracticeCycle.AVANZADO);
        subject.setCode("C".repeat(51));

        assertThrows(IllegalArgumentException.class, () -> subjectService.crearAsignatura(subject));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void crearAsignatura_NonExistentProfessor_ThrowsIllegalArgumentException() {
        Subject subject = new Subject();
        subject.setName("Didáctica");
        subject.setCycle(PracticeCycle.INICIAL);
        User prof = new User();
        prof.setId(99L);
        subject.setProfessor(prof);

        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> subjectService.crearAsignatura(subject));
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void eliminarAsignatura_Success() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(practiceAssignmentRepository.existsBySubjectId(1L)).thenReturn(false);

        subjectService.eliminarAsignatura(1L);

        verify(subjectRepository).deleteById(1L);
    }

    @Test
    void eliminarAsignatura_AssociatedAssignments_ThrowsIllegalStateException() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(practiceAssignmentRepository.existsBySubjectId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> subjectService.eliminarAsignatura(1L));
        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void eliminarAsignatura_NotFound_ThrowsNoSuchElementException() {
        when(subjectRepository.existsById(99L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> subjectService.eliminarAsignatura(99L));
        verify(subjectRepository, never()).deleteById(any());
    }

    @Test
    void listarAsignaturas_ReturnsList() {
        when(subjectRepository.findAll()).thenReturn(List.of(new Subject(), new Subject()));

        List<Subject> result = subjectService.listarAsignaturas();

        assertEquals(2, result.size());
    }
}
