package com.ubb.practicas.model;

import com.ubb.practicas.model.enums.PeriodStatus;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "Periodos_Academicos")
public class AcademicPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer year;
    private Integer semesterNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    private PeriodStatus status;
}
