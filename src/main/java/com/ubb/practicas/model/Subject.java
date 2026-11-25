package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.PracticeCycle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "Asignaturas")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private PracticeCycle cycle;

    private String code;

    /** Configuración explícita: un ciclo avanzado no implica práctica profesional. */
    private Boolean professionalRubrics;

    @PrePersist
    void initializeRubricConfiguration() {
        if (professionalRubrics == null) professionalRubrics = false;
    }

    public Subject(Long id, String name, PracticeCycle cycle, String code, User professor) {
        this.id = id;
        this.name = name;
        this.cycle = cycle;
        this.code = code;
        this.professor = professor;
        this.professionalRubrics = false;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id")
    @ToString.Exclude
    private User professor;
}
