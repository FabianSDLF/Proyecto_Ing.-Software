package com.ubb.practicas.service.impl;

import com.ubb.practicas.dto.PracticeCycleDTO;
import com.ubb.practicas.dto.PracticeCycleUpdateRequest;
import com.ubb.practicas.model.PracticeCycleDefinition;
import com.ubb.practicas.model.Subject;
import com.ubb.practicas.model.enums.PracticeCycle;
import com.ubb.practicas.repository.PracticeCycleDefinitionRepository;
import com.ubb.practicas.repository.SubjectRepository;
import com.ubb.practicas.service.PracticeCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeCycleServiceImpl implements PracticeCycleService {

    /** Datos iniciales de cada ciclo, según el proceso de formación práctica de la carrera. */
    static final Map<PracticeCycle, PracticeCycleDefinition> DEFAULTS = new EnumMap<>(PracticeCycle.class);

    static {
        DEFAULTS.put(PracticeCycle.INICIAL, definicion(PracticeCycle.INICIAL, "Ciclo Inicial",
                "Aproximación al sistema escolar y Ambientes de aprendizaje. Sin clases reales: "
                        + "simulaciones en la universidad, visitas de delegaciones y conversatorios.",
                1, 4, 5));
        DEFAULTS.put(PracticeCycle.INTERMEDIO, definicion(PracticeCycle.INTERMEDIO, "Ciclo Intermedio",
                "Trabajo colaborativo en el aula e Investigación en el aula. Primer contacto real "
                        + "con el aula, en duplas que comparten centro de práctica.",
                2, 6, 7));
        DEFAULTS.put(PracticeCycle.AVANZADO, definicion(PracticeCycle.AVANZADO, "Ciclo Avanzado",
                "Práctica pedagógica (6 semanas) y Práctica profesional (13 semanas). Individual: "
                        + "el estudiante tiene a cargo el curso y la jefatura.",
                3, null, null));
    }

    private final PracticeCycleDefinitionRepository cycleRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public int registrarCiclosFaltantes() {
        int creados = 0;
        for (PracticeCycle code : PracticeCycle.values()) {
            if (!cycleRepository.existsByCode(code)) {
                PracticeCycleDefinition base = DEFAULTS.get(code);
                cycleRepository.save(definicion(code, base.getName(), base.getDescription(),
                        base.getDisplayOrder(), base.getStartSemester(), base.getEndSemester()));
                creados++;
            }
        }
        if (creados > 0) {
            log.info("HU5: se registraron {} ciclo(s) de práctica faltante(s).", creados);
        }
        return creados;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeCycleDTO> listarCiclos() {
        Map<PracticeCycle, List<Subject>> subjectsByCycle = agruparAsignaturas();
        return cycleRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> toDto(c, subjectsByCycle.getOrDefault(c.getCode(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeCycleDTO obtenerCiclo(PracticeCycle code) {
        PracticeCycleDefinition cycle = buscar(code);
        return toDto(cycle, agruparAsignaturas().getOrDefault(code, List.of()));
    }

    @Override
    @Transactional
    public PracticeCycleDTO actualizarCiclo(PracticeCycle code, PracticeCycleUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del ciclo son obligatorios");
        }
        PracticeCycleDefinition cycle = buscar(code);

        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("El nombre del ciclo es obligatorio");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("El nombre del ciclo no puede superar los 100 caracteres");
        }

        String description = request.getDescription() == null ? null : request.getDescription().trim();
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("La descripción no puede superar los 1000 caracteres");
        }

        Integer start = request.getStartSemester();
        Integer end = request.getEndSemester();
        validarSemestre(start, "inicio");
        validarSemestre(end, "término");
        if (start != null && end != null && start > end) {
            throw new IllegalArgumentException("El semestre de inicio no puede ser posterior al semestre de término");
        }

        cycle.setName(name);
        cycle.setDescription(description == null || description.isEmpty() ? null : description);
        cycle.setStartSemester(start);
        cycle.setEndSemester(end);

        PracticeCycleDefinition saved = cycleRepository.save(cycle);
        return toDto(saved, agruparAsignaturas().getOrDefault(code, List.of()));
    }

    // ------------------------------------------------------------------

    private PracticeCycleDefinition buscar(PracticeCycle code) {
        if (code == null) {
            throw new IllegalArgumentException("El código del ciclo es obligatorio");
        }
        return cycleRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("Ciclo de práctica no registrado: " + code));
    }

    private static void validarSemestre(Integer semestre, String etiqueta) {
        if (semestre != null && (semestre < 1 || semestre > 12)) {
            throw new IllegalArgumentException("El semestre de " + etiqueta + " debe estar entre 1 y 12");
        }
    }

    private Map<PracticeCycle, List<Subject>> agruparAsignaturas() {
        return subjectRepository.findAll().stream()
                .filter(s -> s.getCycle() != null)
                .collect(Collectors.groupingBy(Subject::getCycle));
    }

    private static PracticeCycleDTO toDto(PracticeCycleDefinition c, List<Subject> subjects) {
        List<PracticeCycleDTO.SubjectSummary> resumen = subjects.stream()
                .sorted(Comparator.comparing(Subject::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(s -> new PracticeCycleDTO.SubjectSummary(s.getId(), s.getName(), s.getCode()))
                .toList();
        return PracticeCycleDTO.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .description(c.getDescription())
                .displayOrder(c.getDisplayOrder())
                .startSemester(c.getStartSemester())
                .endSemester(c.getEndSemester())
                .updatedAt(c.getUpdatedAt())
                .subjectCount(resumen.size())
                .subjects(resumen)
                .build();
    }

    private static PracticeCycleDefinition definicion(PracticeCycle code, String name, String description,
                                                      Integer order, Integer start, Integer end) {
        PracticeCycleDefinition d = new PracticeCycleDefinition();
        d.setCode(code);
        d.setName(name);
        d.setDescription(description);
        d.setDisplayOrder(order);
        d.setStartSemester(start);
        d.setEndSemester(end);
        return d;
    }
}
