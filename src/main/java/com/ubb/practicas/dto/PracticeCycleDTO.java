package com.ubb.practicas.dto;

import com.ubb.practicas.model.enums.PracticeCycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HU5: Vista de un ciclo de práctica con las asignaturas asociadas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeCycleDTO {
    private Long id;
    private PracticeCycle code;
    private String name;
    private String description;
    private Integer displayOrder;
    private Integer startSemester;
    private Integer endSemester;
    private LocalDateTime updatedAt;
    private int subjectCount;
    private List<SubjectSummary> subjects;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectSummary {
        private Long id;
        private String name;
        private String code;
    }
}
