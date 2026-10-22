package com.ubb.practicas.service;

import com.ubb.practicas.dto.PracticeCycleDTO;
import com.ubb.practicas.dto.PracticeCycleUpdateRequest;
import com.ubb.practicas.model.PracticeCycleDefinition;
import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.PracticeCycleDefinitionRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.service.impl.PracticeCycleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeCycleServiceTest {

    @Mock
    private PracticeCycleDefinitionRepository cycleRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private PracticeCycleServiceImpl service;

    private static PracticeCycleDefinition ciclo(PracticeCycle code, int order) {
        PracticeCycleDefinition c = new PracticeCycleDefinition();
        c.setId((long) order);
        c.setCode(code);
        c.setName("Ciclo " + code.name());
        c.setDisplayOrder(order);
        return c;
    }

    private static Subject asignatura(long id, String name, PracticeCycle cycle) {
        Subject s = new Subject();
        s.setId(id);
        s.setName(name);
        s.setCycle(cycle);
        return s;
    }

    // ---------------- registrarCiclosFaltantes ----------------

    @Test
    void registrarCiclosFaltantes_BaseVacia_CreaLosTres() {
        when(cycleRepository.existsByCode(any())).thenReturn(false);

        int creados = service.registrarCiclosFaltantes();

        assertEquals(3, creados);
        ArgumentCaptor<PracticeCycleDefinition> captor = ArgumentCaptor.forClass(PracticeCycleDefinition.class);
        verify(cycleRepository, times(3)).save(captor.capture());
        List<PracticeCycleDefinition> guardados = captor.getAllValues();
        assertEquals(List.of(PracticeCycle.INICIAL, PracticeCycle.INTERMEDIO, PracticeCycle.AVANZADO),
                guardados.stream().map(PracticeCycleDefinition::getCode).toList());
        assertEquals(List.of(1, 2, 3), guardados.stream().map(PracticeCycleDefinition::getDisplayOrder).toList());
        guardados.forEach(c -> assertFalse(c.getName().isBlank()));
    }

    @Test
    void registrarCiclosFaltantes_YaRegistrados_NoDuplica() {
        when(cycleRepository.existsByCode(any())).thenReturn(true);

        assertEquals(0, service.registrarCiclosFaltantes());
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void registrarCiclosFaltantes_FaltaUno_SoloCreaEse() {
        when(cycleRepository.existsByCode(any())).thenReturn(true);
        when(cycleRepository.existsByCode(PracticeCycle.INTERMEDIO)).thenReturn(false);

        assertEquals(1, service.registrarCiclosFaltantes());
        ArgumentCaptor<PracticeCycleDefinition> captor = ArgumentCaptor.forClass(PracticeCycleDefinition.class);
        verify(cycleRepository).save(captor.capture());
        assertEquals(PracticeCycle.INTERMEDIO, captor.getValue().getCode());
    }

    // ---------------- listar / obtener ----------------

    @Test
    void listarCiclos_AgrupaAsignaturasPorCicloYOrdenaPorNombre() {
        when(cycleRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(
                ciclo(PracticeCycle.INICIAL, 1), ciclo(PracticeCycle.INTERMEDIO, 2), ciclo(PracticeCycle.AVANZADO, 3)));
        when(subjectRepository.findAll()).thenReturn(List.of(
                asignatura(1, "Ambientes de aprendizaje", PracticeCycle.INICIAL),
                asignatura(2, "Aproximación al sistema escolar", PracticeCycle.INICIAL),
                asignatura(3, "Trabajo colaborativo", PracticeCycle.INTERMEDIO),
                asignatura(4, "Sin ciclo", null)));

        List<PracticeCycleDTO> result = service.listarCiclos();

        assertEquals(3, result.size());
        assertEquals(2, result.get(0).getSubjectCount());
        assertEquals("Ambientes de aprendizaje", result.get(0).getSubjects().get(0).getName());
        assertEquals(1, result.get(1).getSubjectCount());
        assertEquals(0, result.get(2).getSubjectCount());
        assertTrue(result.get(2).getSubjects().isEmpty());
    }

    @Test
    void obtenerCiclo_NoRegistrado_LanzaNoSuchElement() {
        when(cycleRepository.findByCode(PracticeCycle.AVANZADO)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.obtenerCiclo(PracticeCycle.AVANZADO));
    }

    @Test
    void obtenerCiclo_CodigoNulo_LanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.obtenerCiclo(null));
    }

    // ---------------- actualizar ----------------

    @Test
    void actualizarCiclo_Exito_RecortaYGuarda() {
        PracticeCycleDefinition existente = ciclo(PracticeCycle.INICIAL, 1);
        when(cycleRepository.findByCode(PracticeCycle.INICIAL)).thenReturn(Optional.of(existente));
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findAll()).thenReturn(List.of());

        PracticeCycleDTO dto = service.actualizarCiclo(PracticeCycle.INICIAL,
                new PracticeCycleUpdateRequest("  Ciclo Inicial PEM  ", "  Descripción nueva  ", 3, 4));

        assertEquals("Ciclo Inicial PEM", dto.getName());
        assertEquals("Descripción nueva", dto.getDescription());
        assertEquals(3, dto.getStartSemester());
        assertEquals(4, dto.getEndSemester());
        assertEquals(PracticeCycle.INICIAL, dto.getCode());
        assertEquals(1, dto.getDisplayOrder());
    }

    @Test
    void actualizarCiclo_DescripcionVacia_SeGuardaComoNula() {
        when(cycleRepository.findByCode(PracticeCycle.INICIAL)).thenReturn(Optional.of(ciclo(PracticeCycle.INICIAL, 1)));
        when(cycleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findAll()).thenReturn(List.of());

        PracticeCycleDTO dto = service.actualizarCiclo(PracticeCycle.INICIAL,
                new PracticeCycleUpdateRequest("Inicial", "   ", null, null));

        assertNull(dto.getDescription());
    }

    @Test
    void actualizarCiclo_NombreVacio_LanzaIllegalArgument() {
        when(cycleRepository.findByCode(PracticeCycle.INICIAL)).thenReturn(Optional.of(ciclo(PracticeCycle.INICIAL, 1)));

        assertThrows(IllegalArgumentException.class, () -> service.actualizarCiclo(PracticeCycle.INICIAL,
                new PracticeCycleUpdateRequest("   ", null, null, null)));
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void actualizarCiclo_SemestreInicioMayorQueTermino_LanzaIllegalArgument() {
        when(cycleRepository.findByCode(PracticeCycle.INTERMEDIO)).thenReturn(Optional.of(ciclo(PracticeCycle.INTERMEDIO, 2)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.actualizarCiclo(PracticeCycle.INTERMEDIO,
                        new PracticeCycleUpdateRequest("Intermedio", null, 7, 6)));
        assertTrue(ex.getMessage().contains("posterior"));
        verify(cycleRepository, never()).save(any());
    }

    @Test
    void actualizarCiclo_SemestreFueraDeRango_LanzaIllegalArgument() {
        when(cycleRepository.findByCode(PracticeCycle.AVANZADO)).thenReturn(Optional.of(ciclo(PracticeCycle.AVANZADO, 3)));

        assertThrows(IllegalArgumentException.class, () -> service.actualizarCiclo(PracticeCycle.AVANZADO,
                new PracticeCycleUpdateRequest("Avanzado", null, 0, 13)));
    }

    @Test
    void actualizarCiclo_RequestNulo_LanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarCiclo(PracticeCycle.INICIAL, null));
    }
}
